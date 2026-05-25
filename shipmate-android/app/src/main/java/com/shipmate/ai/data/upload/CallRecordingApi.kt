package com.shipmate.ai.data.upload

data class CreateUploadSessionRequest(
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val uploadConsent: Boolean,
    val source: String = "android_manual"
)

data class UploadSessionResponse(
    val recordingId: String,
    val uploadUrl: String,
    val objectKey: String,
    val expiresInSeconds: Int
)

data class CompleteUploadResponse(
    val recordingId: String,
    val status: CallRecordingStatus
)

data class RecentCallRecordingResponse(
    val recordingId: String,
    val fileName: String,
    val status: CallRecordingStatus,
    val uploadedAt: String?,
    val createdAt: String? = null,
    val source: String? = null,
    val fileSize: Long? = null,
    val objectKey: String? = null,
    val errorCode: String? = null
)

enum class CallRecordingStatus(val label: String) {
    CREATED("Created"),
    UPLOAD_URL_ISSUED("Ready"),
    UPLOADING("Uploading"),
    UPLOADED("Uploaded"),
    ANALYZING("Analyzing"),
    ANALYSIS_DONE("Analysis done"),
    FAILED("Failed")
}
