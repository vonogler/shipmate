package com.shipmate.ai.data.upload

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

data class CallRecordingFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val contentType: String,
    val dateAddedSeconds: Long
)

class CallRecordingFileFinder(
    private val contentResolver: ContentResolver
) {
    fun findLatestAudioFile(): CallRecordingFile? {
        return findRecentAudioFiles(limit = 1).firstOrNull()
    }

    fun findRecentAudioFiles(limit: Int = 50): List<CallRecordingFile> {
        return findAudioFilesAddedSince(startedAtSeconds = 0L, limit = limit)
    }

    fun findAudioFilesAddedSince(startedAtSeconds: Long, limit: Int = 50): List<CallRecordingFile> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val selection = if (startedAtSeconds > 0L) {
            "${MediaStore.Audio.Media.DATE_ADDED} >= ?"
        } else {
            null
        }
        val selectionArgs = if (startedAtSeconds > 0L) {
            arrayOf(startedAtSeconds.toString())
        } else {
            null
        }

        return contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val files = mutableListOf<CallRecordingFile>()
            while (cursor.moveToNext() && files.size < limit) {
                files.add(cursor.toCallRecordingFile(collection))
            }
            files
        }.orEmpty()
    }

    private fun Cursor.toCallRecordingFile(collection: Uri): CallRecordingFile {
        val id = getLong(getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
        val name = getString(getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
            ?: "auto-call-recording"
        val sizeIndex = getColumnIndex(MediaStore.Audio.Media.SIZE)
        val mimeTypeIndex = getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
        val dateAddedIndex = getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
        return CallRecordingFile(
            uri = Uri.withAppendedPath(collection, id.toString()),
            name = name,
            sizeBytes = if (sizeIndex >= 0) getLong(sizeIndex) else 0L,
            contentType = if (mimeTypeIndex >= 0) {
                getString(mimeTypeIndex) ?: "audio/*"
            } else {
                "audio/*"
            },
            dateAddedSeconds = if (dateAddedIndex >= 0) getLong(dateAddedIndex) else 0L
        )
    }
}
