package com.shipmate.ai.ui.callupload

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shipmate.ai.background.CallUploadMonitorService
import com.shipmate.ai.background.CallUploadToastNotifier
import com.shipmate.ai.background.CallUploadWorkScheduler
import com.shipmate.ai.data.upload.CallRecordingFile
import com.shipmate.ai.data.upload.CallRecordingFileFinder
import com.shipmate.ai.data.upload.CallRecordingStatus
import com.shipmate.ai.data.upload.CallRecordingUploadRepository
import com.shipmate.ai.data.upload.CallUploadSettings
import com.shipmate.ai.data.upload.RecentCallRecordingResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallUploadViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = CallRecordingUploadRepository()
    private var lastBackgroundStatus: CallRecordingStatus? = null
    private var lastBackgroundMessage: String? = null

    private val _uiState = MutableStateFlow(
        CallUploadUiState(
            uploadConsent = CallUploadSettings.hasUploadConsent(application),
            autoUploadConsent = CallUploadSettings.hasAutoUploadConsent(application)
        )
    )
    val uiState: StateFlow<CallUploadUiState> = _uiState.asStateFlow()

    init {
        refreshBackgroundUploadStatus()
        refreshRecentUploads()
        observeBackgroundUploadStatus()
    }

    fun setConsent(consented: Boolean) {
        CallUploadSettings.setUploadConsent(
            context = getApplication<Application>(),
            consented = consented,
            autoUploadConsent = _uiState.value.autoUploadConsent
        )

        _uiState.update {
            it.copy(
                uploadConsent = consented,
                autoUploadConsent = if (consented) it.autoUploadConsent else false,
                errorMessage = null,
            )
        }
        if (!consented) {
            CallUploadMonitorService.stop(getApplication<Application>())
        }
    }

    fun setAutoUploadConsent(consented: Boolean) {
        CallUploadSettings.setAutoUploadConsent(
            context = getApplication<Application>(),
            consented = consented,
            uploadConsent = _uiState.value.uploadConsent
        )

        _uiState.update {
            it.copy(
                uploadConsent = if (consented) true else it.uploadConsent,
                autoUploadConsent = consented,
                errorMessage = null,
            )
        }
        if (!consented) {
            CallUploadMonitorService.stop(getApplication<Application>())
        }
    }

    fun startAutoUploadMonitor() {
        if (_uiState.value.autoUploadConsent) {
            val context = getApplication<Application>()
            CallUploadMonitorService.start(context)
            if (
                CallUploadSettings.isCallUploadPending(context) &&
                CallUploadSettings.getCallStartedAtSeconds(context) > 0L
            ) {
                CallUploadToastNotifier.showWaitingUploadQueued(context)
                CallUploadSettings.setAutoUploadStatus(
                    context = context,
                    status = CallRecordingStatus.UPLOADING.name,
                    message = "권한이 허용되어 대기 중인 통화녹음을 업로드합니다."
                )
                refreshBackgroundUploadStatus()
                CallUploadWorkScheduler.enqueueBackgroundUpload(context, delaySeconds = 0)
            }
        }
    }

    fun dismissAutoUploadConsentDialog() {
        _uiState.update { it.copy(showAutoUploadConsentDialog = false, autoUploadAttempted = true) }
    }

    fun showAutoUploadPermissionExplanation() {
        _uiState.update { it.copy(showAutoUploadPermissionDialog = true) }
    }

    fun dismissAutoUploadPermissionExplanation() {
        _uiState.update { it.copy(showAutoUploadPermissionDialog = false, autoUploadAttempted = true) }
    }

    fun markAutoUploadPermissionRequestStarted() {
        _uiState.update { it.copy(showAutoUploadPermissionDialog = false) }
    }

    fun requestAutoUploadConsentIfNeeded() {
        if (!_uiState.value.autoUploadConsent) {
            _uiState.update { it.copy(showAutoUploadConsentDialog = true) }
        }
    }

    fun acceptAutoUploadConsent() {
        setAutoUploadConsent(true)
        _uiState.update { it.copy(showAutoUploadConsentDialog = false, autoUploadAttempted = false) }
    }

    fun onAutoUploadPermissionDenied() {
        val context = getApplication<Application>()
        val message = """
            필요한 권한이 없어 자동 업로드를 시작할 수 없습니다.

            단계: Android 권한 요청
            필요한 권한: 오디오 파일 접근, 전화 상태, 연락처, Android 버전에 따른 알림 권한
            조치: 권한 요청창에서 허용을 선택해 주세요. 권한창이 더 이상 표시되지 않으면 Android 설정에서 직접 권한을 허용해 주세요.
        """.trimIndent()

        CallUploadToastNotifier.showPermissionRequired(context)
        CallUploadSettings.setAutoUploadStatus(
            context = context,
            status = CallRecordingStatus.FAILED.name,
            message = message
        )
        _uiState.update {
            it.copy(
                autoUploadAttempted = true,
                currentStatus = CallRecordingStatus.FAILED,
                errorMessage = message,
            )
        }
    }

    fun autoUploadLatestCallRecording() {
        val state = _uiState.value
        if (state.autoUploadAttempted || state.isAutoUploading || state.isUploading) return

        if (!state.autoUploadConsent) {
            requestAutoUploadConsentIfNeeded()
            return
        }

        viewModelScope.launch {
            var targetFile: CallRecordingFile? = null
            runCatching {
                targetFile = CallRecordingFileFinder(getApplication<Application>().contentResolver).findLatestAudioFile()
                    ?: error(
                        """
                        자동 업로드할 오디오 파일을 찾지 못했습니다.

                        단계: 최신 오디오 파일 찾기
                        검색 위치: Android MediaStore 오디오 목록
                        조치: 기기에 통화녹음 파일이 있는지 확인하거나 테스트용으로 수동 업로드를 사용해 주세요.
                        """.trimIndent()
                    )

                val file = requireNotNull(targetFile)
                if (CallUploadSettings.isLastAutoUploaded(getApplication<Application>(), file.uri.toString())) {
                    error(
                        """
                        최신 오디오 파일이 이미 업로드되어 자동 업로드를 건너뛰었습니다.

                        단계: 중복 업로드 방지
                        파일: ${file.name}
                        Uri: ${file.uri}
                        크기: ${file.sizeBytes}바이트
                        조치: 새 통화녹음 파일이 추가되었는지 확인하거나 수동 업로드를 사용해 주세요.
                        """.trimIndent()
                    )
                }

                setUploadingState(file)

                repository.uploadSelectedFile(
                    contentResolver = getApplication<Application>().contentResolver,
                    uri = file.uri,
                    fileName = file.name,
                    contentType = file.contentType,
                    fileSize = file.sizeBytes,
                    source = "android_auto",
                    onProgress = { progress ->
                        _uiState.update { it.copy(uploadProgress = progress) }
                    }
                )
            }.onSuccess { recent ->
                handleUploadSuccess(targetFile, recent)
            }.onFailure { throwable ->
                handleUploadFailure(targetFile, throwable)
            }
        }
    }

    fun selectFile(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        val metadata = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            val hasRow = cursor.moveToFirst()
            SelectedCallFile(
                uri = uri,
                name = if (hasRow && nameIndex >= 0) {
                    cursor.getString(nameIndex) ?: "선택한 통화녹음"
                } else {
                    "선택한 통화녹음"
                },
                sizeBytes = if (hasRow && sizeIndex >= 0) {
                    cursor.getLong(sizeIndex)
                } else {
                    0
                },
                contentType = resolver.getType(uri) ?: "audio/*"
            )
        } ?: SelectedCallFile(
            uri = uri,
            name = "선택한 통화녹음",
            sizeBytes = 0,
            contentType = resolver.getType(uri) ?: "audio/*"
        )

        _uiState.update {
            it.copy(
                selectedFile = metadata,
                uploadProgress = 0f,
                errorMessage = null
            )
        }
    }

    fun uploadSelectedFile() {
        val state = _uiState.value
        val file = state.selectedFile

        if (!state.uploadConsent) {
            CallUploadToastNotifier.showPermissionRequired(getApplication<Application>())
            _uiState.update {
                it.copy(errorMessage = "업로드와 분석을 진행하려면 먼저 동의가 필요합니다.")
            }
            return
        }

        if (file == null) {
            CallUploadToastNotifier.showNoRecordingFound(getApplication<Application>())
            _uiState.update {
                it.copy(errorMessage = "먼저 통화녹음 파일을 선택해 주세요.")
            }
            return
        }

        viewModelScope.launch {
            val context = getApplication<Application>()
            _uiState.update {
                it.copy(
                    isUploading = true,
                    uploadProgress = 0f,
                    currentStatus = CallRecordingStatus.UPLOADING,
                    errorMessage = null
                )
            }
            CallUploadSettings.setAutoUploadStatus(
                context = context,
                status = CallRecordingStatus.UPLOADING.name,
                message = "출고표 작성을 위해 통화녹음을 업로드합니다."
            )
            CallUploadToastNotifier.showUploadStarting(context)

            runCatching {
                repository.uploadSelectedFile(
                    contentResolver = context.contentResolver,
                    uri = file.uri,
                    fileName = file.name,
                    contentType = file.contentType,
                    fileSize = file.sizeBytes,
                    onProgress = { progress ->
                        _uiState.update { it.copy(uploadProgress = progress) }
                    }
                )
            }.onSuccess { recent ->
                CallUploadToastNotifier.showUploadCompleted(context)
                CallUploadSettings.setAutoUploadStatus(
                    context = context,
                    status = recent.status.name,
                    message = "출고표 작성을 위한 업로드가 완료되었습니다."
                )
                val latestUploads = runCatching {
                    repository.getRecentUploads()
                }.getOrDefault(listOf(recent) + _uiState.value.recentUploads)

                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadProgress = 1f,
                        currentStatus = recent.status,
                        recentUploads = latestUploads,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                CallUploadToastNotifier.showUploadFailed(context)
                val message = throwable.message ?: "업로드에 실패했습니다."
                CallUploadSettings.setAutoUploadStatus(
                    context = context,
                    status = CallRecordingStatus.FAILED.name,
                    message = message
                )
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        currentStatus = CallRecordingStatus.FAILED,
                        errorMessage = message,
                    )
                }
            }
        }
    }

    private fun observeBackgroundUploadStatus() {
        viewModelScope.launch {
            while (true) {
                delay(1500)
                refreshBackgroundUploadStatus()
            }
        }
    }

    private fun refreshBackgroundUploadStatus() {
        val context = getApplication<Application>()
        val status = CallUploadSettings.getAutoUploadStatus(context)
            ?.let { runCatching { CallRecordingStatus.valueOf(it) }.getOrNull() }
            ?: return
        val message = CallUploadSettings.getAutoUploadStatusMessage(context)
        if (status == lastBackgroundStatus && message == lastBackgroundMessage) {
            return
        }
        lastBackgroundStatus = status
        lastBackgroundMessage = message

        val progress = when (status) {
            CallRecordingStatus.UPLOADING -> 0.5f
            CallRecordingStatus.UPLOADED,
            CallRecordingStatus.TRANSCRIBING,
            CallRecordingStatus.TRANSCRIBED,
            CallRecordingStatus.ANALYZING,
            CallRecordingStatus.ANALYSIS_DONE -> 1f
            else -> _uiState.value.uploadProgress
        }

        _uiState.update {
            it.copy(
                currentStatus = status,
                isAutoUploading = status == CallRecordingStatus.UPLOADING,
                uploadProgress = progress,
                errorMessage = message
            )
        }

        if (status != CallRecordingStatus.UPLOADING) {
            refreshRecentUploads()
        }
    }

    private fun setUploadingState(file: CallRecordingFile) {
        val context = getApplication<Application>()
        _uiState.update {
            it.copy(
                selectedFile = SelectedCallFile(
                    uri = file.uri,
                    name = file.name,
                    sizeBytes = file.sizeBytes,
                    contentType = file.contentType
                ),
                isAutoUploading = true,
                autoUploadAttempted = true,
                uploadProgress = 0f,
                currentStatus = CallRecordingStatus.UPLOADING,
                errorMessage = null,
            )
        }
        CallUploadSettings.setAutoUploadStatus(
            context = context,
            status = CallRecordingStatus.UPLOADING.name,
            message = "출고표 작성을 위해 통화녹음을 업로드합니다."
        )
        CallUploadToastNotifier.showUploadStarting(context)
    }

    private suspend fun handleUploadSuccess(
        targetFile: CallRecordingFile?,
        recent: RecentCallRecordingResponse
    ) {
        val context = getApplication<Application>()
        CallUploadToastNotifier.showUploadCompleted(context)
        targetFile?.let {
            CallUploadSettings.setLastAutoUploadedUri(
                context = context,
                uri = it.uri.toString()
            )
        }
        CallUploadSettings.setAutoUploadStatus(
            context = context,
            status = recent.status.name,
            message = "출고표 작성을 위한 업로드가 완료되었습니다."
        )

        val latestUploads = runCatching {
            repository.getRecentUploads()
        }.getOrDefault(listOf(recent) + _uiState.value.recentUploads)

        _uiState.update {
            it.copy(
                isAutoUploading = false,
                uploadProgress = 1f,
                currentStatus = recent.status,
                recentUploads = latestUploads,
                errorMessage = null
            )
        }
    }

    private fun handleUploadFailure(
        targetFile: CallRecordingFile?,
        throwable: Throwable
    ) {
        val context = getApplication<Application>()
        if (targetFile == null) {
            CallUploadToastNotifier.showNoRecordingFound(context)
        } else if (throwable.message?.contains("이미 업로드") == true) {
            CallUploadToastNotifier.showAlreadyUploaded(context)
        } else {
            CallUploadToastNotifier.showUploadFailed(context)
        }
        val message = buildAutoUploadErrorMessage(
            step = "자동 업로드",
            file = targetFile,
            throwable = throwable
        )
        CallUploadSettings.setAutoUploadStatus(
            context = context,
            status = CallRecordingStatus.FAILED.name,
            message = message
        )
        _uiState.update {
            it.copy(
                isAutoUploading = false,
                autoUploadAttempted = true,
                currentStatus = CallRecordingStatus.FAILED,
                errorMessage = message,
            )
        }
    }

    private fun refreshRecentUploads() {
        viewModelScope.launch {
            runCatching {
                repository.getRecentUploads()
            }.onSuccess { uploads ->
                _uiState.update {
                    it.copy(recentUploads = uploads)
                }
            }
        }
    }

    private fun buildAutoUploadErrorMessage(
        step: String,
        file: CallRecordingFile?,
        throwable: Throwable
    ): String {
        val fileDetails = if (file == null) {
            "파일: 선택되지 않음"
        } else {
            """
            파일: ${file.name}
            Uri: ${file.uri}
            콘텐츠 유형: ${file.contentType}
            크기: ${file.sizeBytes}바이트
            """.trimIndent()
        }
        return """
            자동 업로드에 실패했습니다.

            단계: $step
            $fileDetails
            예외: ${throwable::class.java.simpleName}
            메시지: ${throwable.message ?: "예외 메시지 없음"}
            조치: 오디오 권한, API 서버 접속, MinIO 업로드 URL 접속, 기기 내 파일 존재 여부를 확인해 주세요.
        """.trimIndent()
    }
}

data class CallUploadUiState(
    val uploadConsent: Boolean = false,
    val autoUploadConsent: Boolean = false,
    val selectedFile: SelectedCallFile? = null,
    val isUploading: Boolean = false,
    val isAutoUploading: Boolean = false,
    val autoUploadAttempted: Boolean = false,
    val showAutoUploadConsentDialog: Boolean = false,
    val showAutoUploadPermissionDialog: Boolean = false,
    val uploadProgress: Float = 0f,
    val currentStatus: CallRecordingStatus? = null,
    val recentUploads: List<RecentCallRecordingResponse> = emptyList(),
    val errorMessage: String? = null,
)

data class SelectedCallFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val contentType: String
)
