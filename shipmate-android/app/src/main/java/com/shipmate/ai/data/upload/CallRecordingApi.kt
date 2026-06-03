package com.shipmate.ai.data.upload

data class CreateUploadSessionRequest(
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val uploadConsent: Boolean,
    val source: String = "android_manual"
)

data class ReportUploadFailureRequest(
    val recordingId: String? = null,
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val source: String,
    val errorCode: String,
    val errorMessage: String,
    val failureType: UploadFailureType
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
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val transcript: String? = null,
    val transcribedAt: String? = null
)

enum class UploadFailureType {
    PERMISSION,
    SYSTEM
}

enum class CallRecordingStatus(val label: String) {
    CREATED("생성됨"),
    UPLOAD_URL_ISSUED("업로드 준비"),
    UPLOADING("업로드 중"),
    UPLOADED("업로드 완료"),
    TRANSCRIBING("문자화 중"),
    TRANSCRIBED("문자화 완료"),
    ANALYZING("분석 중"),
    ANALYSIS_DONE("분석 완료"),
    FAILED("실패")
}
