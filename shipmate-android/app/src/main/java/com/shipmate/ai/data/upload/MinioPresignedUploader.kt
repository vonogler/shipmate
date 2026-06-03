package com.shipmate.ai.data.upload

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink

class MinioPresignedUploader(
    private val client: OkHttpClient = OkHttpClient()
) {
    fun upload(
        contentResolver: ContentResolver,
        uri: Uri,
        uploadUrl: String,
        contentType: String,
        contentLength: Long,
        onProgress: (Float) -> Unit
    ) {
        val body = ProgressRequestBody(
            contentResolver = contentResolver,
            uri = uri,
            contentType = contentType,
            contentLength = contentLength,
            onProgress = onProgress
        )

        val request = Request.Builder()
            .url(uploadUrl)
            .put(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("MinIO 업로드에 실패했습니다. HTTP ${response.code}")
            }
        }
    }
}

private class ProgressRequestBody(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val contentType: String,
    private val contentLength: Long,
    private val onProgress: (Float) -> Unit
) : RequestBody() {
    override fun contentType() = contentType.toMediaTypeOrNull()

    override fun contentLength() = if (contentLength > 0) contentLength else -1L

    override fun writeTo(sink: BufferedSink) {
        contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var uploaded = 0L
            var read = input.read(buffer)
            while (read != -1) {
                sink.write(buffer, 0, read)
                uploaded += read
                if (contentLength > 0) {
                    onProgress((uploaded.toFloat() / contentLength).coerceIn(0f, 1f))
                } else {
                    onProgress(0.5f)
                }
                read = input.read(buffer)
            }
        } ?: error("선택한 파일을 열 수 없습니다.")
    }
}
