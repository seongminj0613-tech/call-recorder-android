package com.callrecorder.app.data.repository

import android.util.Log
import com.callrecorder.app.data.api.ApiService
import com.callrecorder.app.data.local.RecordingDao
import com.callrecorder.app.data.local.RecordingEntity
import com.callrecorder.app.data.local.RecordingStatus
import com.callrecorder.app.data.model.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CallRepository(
    private val api: ApiService,
    private val dao: RecordingDao,
) {
    /** 새 녹음을 DB에 등록 (이미 있으면 ignore). 반환값은 로컬 PK(Long). */
    suspend fun registerLocal(rec: RecordingEntity): Long {
        dao.findByPath(rec.filePath)?.let { return it.id }
        return dao.insert(rec)
    }

    suspend fun pendingUploads() = dao.pending()
    fun observeAll() = dao.observeAll()

    /** 한 건 업로드 → 서버 처리 트리거까지. 반환값은 서버 callId(String, UUID). */
    suspend fun uploadAndProcess(rec: RecordingEntity): Result<String> = runCatching {
        val file = File(rec.filePath)
        require(file.exists()) { "파일이 사라졌습니다: ${rec.filePath}" }

        dao.updateStatus(rec.id, RecordingStatus.UPLOADING)

        // 1) Presigned URL 발급
        val mime = guessMime(file.extension)
        val urlResp = api.requestUploadUrl(
            UploadUrlRequest(
                storeId = rec.storeId,
                fileName = rec.fileName,
                fileSize = rec.fileSize,
                mimeType = mime,
                callStartedAt = isoFormat(rec.callStartedAtMillis),
                durationSeconds = rec.durationSeconds,
                counterpartNumber = rec.counterpartNumber,
            )
        )

        // 2) S3 PUT 업로드
        val body = file.asRequestBody(mime.toMediaTypeOrNull())
        val headers = urlResp.uploadHeaders.toMutableMap()
        if (!headers.containsKey("Content-Type")) headers["Content-Type"] = mime
        val s3Resp = api.uploadToS3(urlResp.uploadUrl, headers, body)
        if (!s3Resp.isSuccessful) {
            error("S3 업로드 실패: HTTP ${s3Resp.code()}")
        }

        dao.setServerCallId(rec.id, urlResp.callId, RecordingStatus.UPLOADED)

        // 3) STT/요약 처리 트리거
        val procResp = api.processCall(urlResp.callId)
        if (!procResp.isSuccessful) {
            Log.w("CallRepo", "process trigger failed: ${procResp.code()}")
            // 실패해도 업로드는 됐으므로 PROCESSING 으로만 마킹
        }
        dao.updateStatus(rec.id, RecordingStatus.PROCESSING)
        urlResp.callId
    }.onFailure { e ->
        dao.setError(rec.id, RecordingStatus.FAILED, e.message)
    }

    suspend fun listCalls(storeId: String?): Result<List<Call>> = runCatching {
        api.listCalls(storeId).calls
    }

    suspend fun getDetail(callId: String): Result<CallDetail> = runCatching {
        api.getCall(callId)
    }

    suspend fun getSummary(callId: String): Result<Summary> = runCatching {
        api.getSummary(callId)
    }

    private fun guessMime(ext: String) = when (ext.lowercase()) {
        "m4a", "aac" -> "audio/mp4"
        "mp3" -> "audio/mpeg"
        "amr" -> "audio/amr"
        "3gp" -> "audio/3gpp"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        else -> "audio/mpeg"
    }

    private fun isoFormat(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }
}