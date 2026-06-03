package com.shipmate.ai.data.upload

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CallRecordingApiService {
    @POST("api/call-recordings/upload-sessions")
    suspend fun createUploadSession(
        @Body request: CreateUploadSessionRequest
    ): UploadSessionResponse

    @POST("api/call-recordings/{recordingId}/complete")
    suspend fun completeUpload(
        @Path("recordingId") recordingId: String
    ): CompleteUploadResponse

    @POST("api/call-recordings/upload-failures")
    suspend fun reportUploadFailure(
        @Body request: ReportUploadFailureRequest
    ): RecentCallRecordingResponse

    @GET("api/call-recordings/recent")
    suspend fun getRecentUploads(): List<RecentCallRecordingResponse>
}
