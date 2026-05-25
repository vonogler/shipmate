package com.shipmate.ai.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shipmate.ai.data.upload.CallUploadSettings

class DeviceBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (CallUploadSettings.hasAutoUploadConsent(context)) {
            CallUploadMonitorService.start(context)
        }
    }
}
