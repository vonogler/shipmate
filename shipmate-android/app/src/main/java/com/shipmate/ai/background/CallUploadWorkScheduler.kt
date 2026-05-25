package com.shipmate.ai.background

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration

object CallUploadWorkScheduler {
    fun enqueueBackgroundUpload(context: Context, delaySeconds: Long = 8) {
        val request = OneTimeWorkRequestBuilder<BackgroundCallUploadWorker>()
            .setInitialDelay(Duration.ofSeconds(delaySeconds))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(30))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "call-recording-auto-upload",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
