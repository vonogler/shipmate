package com.shipmate.ai.data.upload

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallRecordingUploadRepository(
    private val apiService: CallRecordingApiService = ShipmateApiClient.callRecordingApiService,
    private val uploader: MinioPresignedUploader = MinioPresignedUploader()
) {
    suspend fun uploadSelectedFile(
        contentResolver: ContentResolver,
        uri: Uri,
        fileName: String,
        contentType: String,
        fileSize: Long,
        source: String = "android_manual",
        onProgress: (Float) -> Unit
    ): RecentCallRecordingResponse {
        return withContext(Dispatchers.IO) {
            val request = CreateUploadSessionRequest(
                fileName = fileName,
                contentType = normalizeAudioContentType(contentType),
                fileSize = fileSize,
                uploadConsent = true,
                source = source
            )

            val session = apiService.createUploadSession(request)
            uploader.upload(
                contentResolver = contentResolver,
                uri = uri,
                uploadUrl = session.uploadUrl,
                contentType = request.contentType,
                contentLength = fileSize,
                onProgress = onProgress
            )
            val completed = apiService.completeUpload(session.recordingId)

            apiService.getRecentUploads()
                .firstOrNull { it.recordingId == completed.recordingId }
                ?: RecentCallRecordingResponse(
                    recordingId = completed.recordingId,
                    fileName = fileName,
                    status = completed.status,
                    uploadedAt = null
                )
        }
    }

    suspend fun getRecentUploads(): List<RecentCallRecordingResponse> {
        return withContext(Dispatchers.IO) {
            apiService.getRecentUploads()
        }
    }

    private fun normalizeAudioContentType(contentType: String): String {
        return if (contentType == "audio/*") "audio/mpeg" else contentType
    }
}
