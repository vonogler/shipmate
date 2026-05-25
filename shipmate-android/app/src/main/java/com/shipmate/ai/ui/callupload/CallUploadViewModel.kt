package com.shipmate.ai.ui.callupload

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shipmate.ai.data.upload.CallRecordingStatus
import com.shipmate.ai.data.upload.CallRecordingUploadRepository
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

    private val _uiState = MutableStateFlow(CallUploadUiState())
    val uiState: StateFlow<CallUploadUiState> = _uiState.asStateFlow()

    init {
        refreshRecentUploads()
    }

    fun setConsent(consented: Boolean) {
        _uiState.update { it.copy(uploadConsent = consented, errorMessage = null) }
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
            _uiState.update {
                it.copy(errorMessage = "Consent is required before upload and AI analysis.")
            }
            return
        }

        if (file == null) {
            _uiState.update {
                it.copy(errorMessage = "Select a call recording file first.")
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
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        currentStatus = CallRecordingStatus.FAILED,
                        errorMessage = throwable.message ?: "Upload failed."
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
}

data class CallUploadUiState(
    val uploadConsent: Boolean = false,
    val selectedFile: SelectedCallFile? = null,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val currentStatus: CallRecordingStatus? = null,
    val recentUploads: List<RecentCallRecordingResponse> = emptyList(),
    val errorMessage: String? = null
)

data class SelectedCallFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val contentType: String
)
