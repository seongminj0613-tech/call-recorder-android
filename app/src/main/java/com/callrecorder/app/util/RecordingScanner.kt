package com.callrecorder.app.util

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

/**
 * 통화 녹음 파일 스캐너.
 *
 * Android는 표준화된 통화 녹음 위치가 없어서, 제조사별 경로를 모두 훑고
 * MediaStore도 활용한다.
 *
 * 주요 경로:
 *  - 삼성: /storage/emulated/0/Recordings/Call/  또는 /Sounds/CallRecord
 *  - LG: /storage/emulated/0/CallRecord/
 *  - 샤오미: /storage/emulated/0/MIUI/sound_recorder/call_rec/
 *  - 화웨이: /storage/emulated/0/Sounds/CallRecord/
 *  - 구글 Recorder: /storage/emulated/0/Recordings/  (Pixel)
 */
data class FoundRecording(
    val file: File,
    val uri: Uri?,
    val durationSeconds: Int,
    val callStartedAtMillis: Long,
    val counterpartNumber: String?,
)

class RecordingScanner(private val context: Context) {

    private val candidateDirs: List<File> = listOf(
        // 삼성
        File(Environment.getExternalStorageDirectory(), "Recordings/Call"),
        File(Environment.getExternalStorageDirectory(), "Sounds/CallRecord"),
        // LG / 일부 베가
        File(Environment.getExternalStorageDirectory(), "CallRecord"),
        // 샤오미
        File(Environment.getExternalStorageDirectory(), "MIUI/sound_recorder/call_rec"),
        // 화웨이
        File(Environment.getExternalStorageDirectory(), "Sounds/CallRecord"),
        // 구글 픽셀 Recorder
        File(Environment.getExternalStorageDirectory(), "Recordings"),
        // 일반적
        File(Environment.getExternalStorageDirectory(), "Call"),
    )

    private val audioExtensions = setOf("m4a", "mp3", "amr", "3gp", "wav", "aac", "ogg")

    /**
     * 두 가지 경로로 스캔 후 머지:
     * 1) 알려진 디렉토리 직접 워킹
     * 2) MediaStore 쿼리 (Android 10+에서 권장)
     */
    fun scan(sinceMillis: Long = 0L): List<FoundRecording> {
        val collected = mutableMapOf<String, FoundRecording>()

        // (1) 디렉토리 워킹
        candidateDirs.forEach { dir ->
            try {
                if (dir.exists() && dir.isDirectory) {
                    dir.walkTopDown().forEach { f ->
                        if (f.isFile
                            && f.extension.lowercase() in audioExtensions
                            && f.lastModified() >= sinceMillis
                            && looksLikeCallRecording(f.name)
                        ) {
                            val info = extract(f, null)
                            collected[f.absolutePath] = info
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "scan dir denied: ${dir.path}", e)
            }
        }

        // (2) MediaStore 쿼리
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            queryMediaStore(sinceMillis).forEach { collected[it.file.absolutePath] = it }
        }

        return collected.values.sortedBy { it.callStartedAtMillis }
    }

    private fun looksLikeCallRecording(name: String): Boolean {
        val lower = name.lowercase()
        // 흔한 파일명 패턴: "통화 녹음", "call", "recording", 전화번호 포함
        if (lower.contains("call") || lower.contains("voice") || lower.contains("rec")) return true
        if (name.contains("통화") || name.contains("녹음")) return true
        // 숫자만(전화번호) 시작하는 패턴
        return Regex("""\d{3,}""").containsMatchIn(name)
    }

    private fun queryMediaStore(sinceMillis: Long): List<FoundRecording> {
        val results = mutableListOf<FoundRecording>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.RELATIVE_PATH,
        )
        val sinceSec = sinceMillis / 1000
        val selection = "${MediaStore.Audio.Media.DATE_MODIFIED} >= ?"
        val args = arrayOf(sinceSec.toString())

        try {
            context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val dataIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val nameIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dateIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val durIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val relIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)

                while (c.moveToNext()) {
                    val name = c.getString(nameIdx) ?: continue
                    val rel = c.getString(relIdx) ?: ""
                    val isCall =
                        rel.contains("Call", ignoreCase = true) ||
                        rel.contains("Recording", ignoreCase = true) ||
                        rel.contains("call_rec", ignoreCase = true) ||
                        looksLikeCallRecording(name)
                    if (!isCall) continue

                    val data = c.getString(dataIdx)
                    val file = if (!data.isNullOrBlank()) File(data) else continue
                    val id = c.getLong(idIdx)
                    val uri = ContentUris.withAppendedId(collection, id)
                    val date = c.getLong(dateIdx) * 1000L
                    val durMs = c.getLong(durIdx)

                    results += FoundRecording(
                        file = file,
                        uri = uri,
                        durationSeconds = (durMs / 1000).toInt(),
                        callStartedAtMillis = date - durMs,
                        counterpartNumber = parseNumberFromName(name),
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore query failed", e)
        }
        return results
    }

    private fun extract(file: File, uri: Uri?): FoundRecording {
        var duration = 0
        try {
            MediaMetadataRetriever().use { mmr ->
                mmr.setDataSource(file.absolutePath)
                duration = (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L).div(1000).toInt()
            }
        } catch (e: Exception) {
            SafeLog.w(TAG, "metadata fail: ${file.name}", e)
        }
        val mod = file.lastModified()
        return FoundRecording(
            file = file,
            uri = uri,
            durationSeconds = duration,
            callStartedAtMillis = mod - duration * 1000L,
            counterpartNumber = parseNumberFromName(file.name),
        )
    }

    private fun parseNumberFromName(name: String): String? {
        // 파일명에서 11자리 한국 휴대폰 번호 패턴 추출
        return Regex("""(010[-.\s]?\d{3,4}[-.\s]?\d{4})""").find(name)
            ?.value?.replace(Regex("""[-.\s]"""), "")
    }

    private fun MediaMetadataRetriever.use(block: (MediaMetadataRetriever) -> Unit) {
        try { block(this) } finally { release() }
    }

    companion object {
        private const val TAG = "RecordingScanner"
    }
}
