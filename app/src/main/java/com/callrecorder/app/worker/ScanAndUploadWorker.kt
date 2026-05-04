package com.callrecorder.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.callrecorder.app.CallRecorderApp
import com.callrecorder.app.data.local.RecordingEntity
import com.callrecorder.app.data.local.RecordingStatus

/**
 * 주기적 스캔 워커 - 15분마다 실행되어
 * 1) 새로 생긴 녹음 파일을 DB에 등록
 * 2) PENDING/FAILED 상태의 모든 녹음을 업로드
 */
class ScanAndUploadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CallRecorderApp
        val scanner = app.container.scanner
        val callRepo = app.container.callRepo
        val tokenStore = app.container.tokenStore

        // 로그인되어 있고 활성 가게가 있을 때만 동작
        val storeId = tokenStore.getActiveStore() ?: return Result.success()
        if (tokenStore.getAccessToken().isNullOrBlank()) return Result.success()

        // 1) 스캔
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val found = scanner.scan(sinceMillis = sevenDaysAgo)

        // 2) DB 등록
        found.forEach { f ->
            callRepo.registerLocal(
                RecordingEntity(
                    filePath = f.file.absolutePath,
                    fileName = f.file.name,
                    fileSize = f.file.length(),
                    durationSeconds = f.durationSeconds,
                    callStartedAtMillis = f.callStartedAtMillis,
                    counterpartNumber = f.counterpartNumber,
                    storeId = storeId,
                    status = RecordingStatus.PENDING,
                )
            )
        }

        // 3) 업로드
        val pending = callRepo.pendingUploads()
        var allOk = true
        pending.forEach {
            val r = callRepo.uploadAndProcess(it)
            if (r.isFailure) allOk = false
        }
        return if (allOk) Result.success() else Result.retry()
    }
}
