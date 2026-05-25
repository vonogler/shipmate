package com.shipmate.ai.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shipmate.ai.data.upload.CallRecordingFileFinder
import com.shipmate.ai.data.upload.CallRecordingUploadRepository
import com.shipmate.ai.data.upload.CallUploadSettings

class BackgroundCallUploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    private val repository = CallRecordingUploadRepository()

    override suspend fun doWork(): Result {
        if (!CallUploadSettings.hasAutoUploadConsent(applicationContext)) {
            return Result.success()
        }

        if (!CallUploadSettings.isCallUploadPending(applicationContext)) {
            return Result.success()
        }

        val callStartedAtSeconds = CallUploadSettings.getCallStartedAtSeconds(applicationContext)
        if (callStartedAtSeconds <= 0L) {
            CallUploadSettings.setCallUploadPending(applicationContext, false)
            return Result.success()
        }

        val files = CallRecordingFileFinder(applicationContext.contentResolver)
            .findAudioFilesAddedSince(startedAtSeconds = callStartedAtSeconds)
            .asReversed()
            .filterNot { CallUploadSettings.isLastAutoUploaded(applicationContext, it.uri.toString()) }

        if (files.isEmpty()) {
            val waitedSeconds = System.currentTimeMillis() / 1000L - callStartedAtSeconds
            if (waitedSeconds > RECORDING_SAVE_TIMEOUT_SECONDS) {
                CallUploadSettings.setCallUploadPending(applicationContext, false)
                return Result.success()
            }
            return Result.retry()
        }

        var hadFailure = false
        files.forEach { file ->
            runCatching {
                repository.uploadSelectedFile(
                    contentResolver = applicationContext.contentResolver,
                    uri = file.uri,
                    fileName = file.name,
                    contentType = file.contentType,
                    fileSize = file.sizeBytes,
                    source = "android_auto",
                    onProgress = {}
                )
            }.onSuccess {
                CallUploadSettings.setLastAutoUploadedUri(applicationContext, file.uri.toString())
            }.onFailure {
                hadFailure = true
            }
        }

        CallUploadSettings.setCallUploadPending(applicationContext, false)
        return if (hadFailure) Result.retry() else Result.success()
    }

    private companion object {
        private const val RECORDING_SAVE_TIMEOUT_SECONDS = 10 * 60
    }
}
