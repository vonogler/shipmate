package com.shipmate.ai.ui.callupload

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shipmate.ai.background.CallUploadMonitorService
import com.shipmate.ai.data.upload.CallRecordingStatus
import com.shipmate.ai.data.upload.CallRecordingFile
import com.shipmate.ai.data.upload.CallRecordingFileFinder
import com.shipmate.ai.data.upload.CallRecordingUploadRepository
import com.shipmate.ai.data.upload.CallUploadSettings
import com.shipmate.ai.data.upload.RecentCallRecordingResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallUploadViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = CallRecordingUploadRepository()

    private val _uiState = MutableStateFlow(
        CallUploadUiState(
            uploadConsent = CallUploadSettings.hasUploadConsent(application),
            autoUploadConsent = CallUploadSettings.hasAutoUploadConsent(application)
        )
    )
    val uiState: StateFlow<CallUploadUiState> = _uiState.asStateFlow()

    init {
        refreshRecentUploads()
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
            CallUploadSettings.setCallUploadPending(getApplication<Application>(), false)
            CallUploadMonitorService.start(getApplication<Application>())
        }
    }

    fun dismissAutoUploadConsentDialog() {
        _uiState.update { it.copy(showAutoUploadConsentDialog = false, autoUploadAttempted = true) }
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
        val message = """
            Automatic upload could not start because audio file access permission was denied.

            Step: Android permission request
            Required permission: audio file read access
            Action: Allow audio access and reopen the app, or upload a call recording manually.
        """.trimIndent()
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
                        No audio file was found for automatic upload.

                        Step: Find latest audio file
                        Search location: Android MediaStore audio library
                        Action: Check that call recordings exist on this device, or use manual upload for testing.
                        """.trimIndent()
                    )

                val file = requireNotNull(targetFile)
                if (CallUploadSettings.isLastAutoUploaded(getApplication<Application>(), file.uri.toString())) {
                    error(
                        """
                        Automatic upload skipped because the latest audio file was already uploaded.

                        Step: Duplicate upload guard
                        File: ${file.name}
                        Uri: ${file.uri}
                        Size: ${file.sizeBytes} bytes
                        Action: Add a new call recording file or use manual upload for retesting the same file.
                        """.trimIndent()
                    )
                }

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
                targetFile?.let {
                    CallUploadSettings.setLastAutoUploadedUri(
                        context = getApplication<Application>(),
                        uri = it.uri.toString()
                    )
                }

                val latestUploads = runCatching {
                    repository.getRecentUploads()
                }.getOrDefault(listOf(recent) + _uiState.value.recentUploads)

                _uiState.update {
                    it.copy(
                        isAutoUploading = false,
                        uploadProgress = 1f,
                        currentStatus = recent.status,
                        recentUploads = latestUploads
                    )
                }
            }.onFailure { throwable ->
                val message = buildAutoUploadErrorMessage(
                    step = "Automatic upload",
                    file = targetFile,
                    throwable = throwable
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
                    cursor.getString(nameIndex) ?: "selected-call-recording"
                } else {
                    "selected-call-recording"
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
            name = "selected-call-recording",
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
            val message = "Consent is required before upload and AI analysis."
            _uiState.update {
                it.copy(errorMessage = message)
            }
            return
        }

        if (file == null) {
            val message = "Select a call recording file first."
            _uiState.update {
                it.copy(errorMessage = message)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploading = true,
                    uploadProgress = 0f,
                    currentStatus = CallRecordingStatus.UPLOADING,
                    errorMessage = null
                )
            }

            runCatching {
                repository.uploadSelectedFile(
                    contentResolver = getApplication<Application>().contentResolver,
                    uri = file.uri,
                    fileName = file.name,
                    contentType = file.contentType,
                    fileSize = file.sizeBytes,
                    onProgress = { progress ->
                        _uiState.update { it.copy(uploadProgress = progress) }
                    }
                )
            }.onSuccess { recent ->
                val latestUploads = runCatching {
                    repository.getRecentUploads()
                }.getOrDefault(listOf(recent) + _uiState.value.recentUploads)

                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadProgress = 1f,
                        currentStatus = recent.status,
                        recentUploads = latestUploads
                    )
                }
            }.onFailure { throwable ->
                val message = throwable.message ?: "Upload failed."
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
            "File: not selected"
        } else {
            """
            File: ${file.name}
            Uri: ${file.uri}
            Content type: ${file.contentType}
            Size: ${file.sizeBytes} bytes
            """.trimIndent()
        }
        return """
            Automatic upload failed.

            Step: $step
            $fileDetails
            Exception: ${throwable::class.java.simpleName}
            Message: ${throwable.message ?: "No exception message"}
            Action: Check audio permission, API server access, MinIO presigned upload access, and whether the file still exists on the device.
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
