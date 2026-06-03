package com.shipmate.ai.background

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object CallUploadToastNotifier {
    fun showUploadStarting(context: Context) {
        show(context, "출고표 작성을 위해 통화녹음을 업로드합니다.")
    }

    fun showUploadCompleted(context: Context) {
        show(context, "출고표 작성을 위한 업로드가 완료되었습니다.")
    }

    fun showUploadFailed(context: Context) {
        show(context, "출고표 작성을 위한 업로드에 실패했습니다.")
    }

    fun showPermissionRequired(context: Context) {
        show(context, "출고표 작성을 위해 자동 업로드 권한이 필요합니다.")
    }

    fun showWaitingUploadQueued(context: Context) {
        show(context, "권한이 허용되어 대기 중인 통화녹음을 업로드합니다.")
    }

    fun showAlreadyUploaded(context: Context) {
        show(context, "이미 업로드된 통화녹음입니다.")
    }

    fun showNoRecordingFound(context: Context) {
        show(context, "업로드할 통화녹음을 찾지 못했습니다.")
    }

    private fun show(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }
}
