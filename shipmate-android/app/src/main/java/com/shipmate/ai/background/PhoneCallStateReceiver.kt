package com.shipmate.ai.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.shipmate.ai.data.upload.CallUploadSettings

class PhoneCallStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_RINGING,
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                CallUploadSettings.markCallStarted(context)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (
                    CallUploadSettings.isCallUploadPending(context) &&
                    CallUploadSettings.getCallStartedAtSeconds(context) > 0L
                ) {
                    val missingRequirements = CallUploadPermissionStatus.missingRequirements(context)
                    if (missingRequirements.isEmpty()) {
                        CallUploadWorkScheduler.enqueueBackgroundUpload(context)
                    } else {
                        CallUploadPermissionNotifier.notifyMissingRequirements(context, missingRequirements)
                    }
                }
            }
        }
    }
}
