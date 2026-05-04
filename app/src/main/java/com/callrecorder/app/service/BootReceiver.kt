package com.callrecorder.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.callrecorder.app.worker.UploadWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            // 옵저버 서비스 재시작 + 주기적 워커 등록
            RecordingObserverService.start(context)
            UploadWorker.enqueuePeriodic(context)
        }
    }
}
