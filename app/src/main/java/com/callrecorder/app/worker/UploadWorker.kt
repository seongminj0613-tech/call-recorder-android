package com.callrecorder.app.worker

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.callrecorder.app.CallRecorderApp
import com.callrecorder.app.MainActivity
import com.callrecorder.app.R
import com.callrecorder.app.data.local.RecordingStatus
import java.util.concurrent.TimeUnit

/**
 * 등록된 PENDING/FAILED 녹음을 모두 업로드한다.
 * - WiFi 연결 시 자동 트리거
 * - 실패 시 지수 백오프 재시도
 */
class UploadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CallRecorderApp
        val repo = app.container.callRepo

        setForeground(buildForegroundInfo("녹음 업로드 중..."))

        // 모든 카테고리 업로드 (마스킹 정책으로 프라이버시 보호)
        val pending = repo.pendingUploads()
        if (pending.isEmpty()) return Result.success()

        var allOk = true
        pending.forEachIndexed { idx, rec ->
            setForeground(buildForegroundInfo("업로드 중 (${idx + 1}/${pending.size})"))
            val r = repo.uploadAndProcess(rec)
            if (r.isFailure) allOk = false
        }
        return if (allOk) Result.success() else Result.retry()
    }

    private fun buildForegroundInfo(text: String): ForegroundInfo {
        val intent = PendingIntent.getActivity(
            applicationContext, 0,
            android.content.Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif: Notification = NotificationCompat.Builder(applicationContext, CallRecorderApp.CHANNEL_UPLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("통화 비서")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(intent)
            .build()
        return if (android.os.Build.VERSION.SDK_INT >= 34) {
            ForegroundInfo(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIF_ID, notif)
        }
    }

    companion object {
        private const val NOTIF_ID = 4001
        const val UNIQUE_NAME = "upload_recordings"

        /** 즉시 1회 업로드 시도 (감지 직후 호출) */
        fun enqueueOneShot(context: Context) {
            val req = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, req
            )
        }

        /** 15분 주기 정기 스캔 + 업로드 (앱이 닫혀 있어도 동작) */
        fun enqueuePeriodic(context: Context) {
            val req = PeriodicWorkRequestBuilder<ScanAndUploadWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "periodic_scan", ExistingPeriodicWorkPolicy.KEEP, req
            )
        }
    }
}
