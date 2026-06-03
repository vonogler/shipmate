package com.shipmate.ai.background

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.shipmate.ai.data.upload.CallUploadSettings

object CallUploadPermissionStatus {
    fun canAutoUpload(context: Context): Boolean {
        return missingRequirements(context).isEmpty()
    }

    fun missingRequirements(context: Context): List<String> {
        val missing = mutableListOf<String>()

        if (!CallUploadSettings.hasAutoUploadConsent(context)) {
            missing.add("automatic upload consent")
        }
        if (!hasPermission(context, audioPermission())) {
            missing.add("audio file access")
        }
        if (!hasPermission(context, Manifest.permission.READ_PHONE_STATE)) {
            missing.add("phone state access")
        }
        if (!hasPermission(context, Manifest.permission.READ_CONTACTS)) {
            missing.add("contacts access")
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            missing.add("notification access")
        }

        return missing
    }

    private fun audioPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
