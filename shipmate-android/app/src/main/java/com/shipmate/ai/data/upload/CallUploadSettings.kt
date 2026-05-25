package com.shipmate.ai.data.upload

import android.content.Context
import android.content.SharedPreferences

object CallUploadSettings {
    private const val PREFERENCES_NAME = "call_upload_preferences"
    private const val KEY_UPLOAD_CONSENT = "upload_consent"
    private const val KEY_AUTO_UPLOAD_CONSENT = "auto_upload_consent"
    private const val KEY_LAST_AUTO_UPLOADED_URI = "last_auto_uploaded_uri"
    private const val KEY_AUTO_UPLOADED_URIS = "auto_uploaded_uris"
    private const val KEY_CALL_UPLOAD_PENDING = "call_upload_pending"
    private const val KEY_CALL_STARTED_AT_SECONDS = "call_started_at_seconds"

    fun preferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    fun hasUploadConsent(context: Context): Boolean {
        return preferences(context).getBoolean(KEY_UPLOAD_CONSENT, false)
    }

    fun hasAutoUploadConsent(context: Context): Boolean {
        return preferences(context).getBoolean(KEY_AUTO_UPLOAD_CONSENT, false)
    }

    fun setUploadConsent(context: Context, consented: Boolean, autoUploadConsent: Boolean) {
        preferences(context).edit()
            .putBoolean(KEY_UPLOAD_CONSENT, consented)
            .putBoolean(KEY_AUTO_UPLOAD_CONSENT, if (consented) autoUploadConsent else false)
            .apply()
    }

    fun setAutoUploadConsent(context: Context, consented: Boolean, uploadConsent: Boolean) {
        preferences(context).edit()
            .putBoolean(KEY_UPLOAD_CONSENT, if (consented) true else uploadConsent)
            .putBoolean(KEY_AUTO_UPLOAD_CONSENT, consented)
            .apply()
    }

    fun isLastAutoUploaded(context: Context, uri: String): Boolean {
        return preferences(context).getString(KEY_LAST_AUTO_UPLOADED_URI, null) == uri ||
            preferences(context).getStringSet(KEY_AUTO_UPLOADED_URIS, emptySet()).orEmpty().contains(uri)
    }

    fun setLastAutoUploadedUri(context: Context, uri: String) {
        val uploadedUris = preferences(context)
            .getStringSet(KEY_AUTO_UPLOADED_URIS, emptySet())
            .orEmpty()
            .toMutableSet()
        uploadedUris.add(uri)

        preferences(context).edit()
            .putString(KEY_LAST_AUTO_UPLOADED_URI, uri)
            .putStringSet(KEY_AUTO_UPLOADED_URIS, uploadedUris)
            .apply()
    }

    fun setCallUploadPending(context: Context, pending: Boolean) {
        preferences(context).edit()
            .putBoolean(KEY_CALL_UPLOAD_PENDING, pending)
            .apply()
    }

    fun isCallUploadPending(context: Context): Boolean {
        return preferences(context).getBoolean(KEY_CALL_UPLOAD_PENDING, false)
    }

    fun markCallStarted(context: Context, startedAtSeconds: Long = System.currentTimeMillis() / 1000L) {
        preferences(context).edit()
            .putBoolean(KEY_CALL_UPLOAD_PENDING, true)
            .putLong(KEY_CALL_STARTED_AT_SECONDS, startedAtSeconds)
            .apply()
    }

    fun getCallStartedAtSeconds(context: Context): Long {
        return preferences(context).getLong(KEY_CALL_STARTED_AT_SECONDS, 0L)
    }
}
