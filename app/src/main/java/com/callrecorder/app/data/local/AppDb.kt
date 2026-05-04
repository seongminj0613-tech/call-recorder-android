package com.callrecorder.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 디바이스에서 감지된 녹음 파일의 업로드 상태를 추적.
 * 같은 파일이 두 번 업로드되지 않도록 file_path + file_size 로 멱등성 보장.
 *
 * 주의: id는 로컬 PK(Long auto-increment), 서버 ID(storeId, serverCallId)는 String(UUID).
 */
@Entity(
    tableName = "recordings",
    indices = [Index(value = ["filePath"], unique = true)]
)
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,    // 로컬 PK는 Long 유지
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val durationSeconds: Int,
    val callStartedAtMillis: Long,
    val counterpartNumber: String?,
    val storeId: String,                                  // Long → String (서버 UUID)
    val status: String,                                   // PENDING / UPLOADING / UPLOADED / PROCESSING / DONE / FAILED
    val serverCallId: String? = null,                     // Long? → String? (서버 UUID)
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

object RecordingStatus {
    const val PENDING = "PENDING"
    const val UPLOADING = "UPLOADING"
    const val UPLOADED = "UPLOADED"
    const val PROCESSING = "PROCESSING"
    const val DONE = "DONE"
    const val FAILED = "FAILED"
}

@Dao
interface RecordingDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rec: RecordingEntity): Long

    @Update
    suspend fun update(rec: RecordingEntity)

    @Query("SELECT * FROM recordings WHERE filePath = :path LIMIT 1")
    suspend fun findByPath(path: String): RecordingEntity?

    @Query("SELECT * FROM recordings ORDER BY callStartedAtMillis DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE status IN ('PENDING','FAILED') ORDER BY createdAt ASC")
    suspend fun pending(): List<RecordingEntity>

    @Query("UPDATE recordings SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, now: Long = System.currentTimeMillis())

    // callId: Long → String
    @Query("UPDATE recordings SET serverCallId = :callId, status = :status, updatedAt = :now WHERE id = :id")
    suspend fun setServerCallId(id: Long, callId: String, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE recordings SET status = :status, errorMessage = :err, updatedAt = :now WHERE id = :id")
    suspend fun setError(id: Long, status: String, err: String?, now: Long = System.currentTimeMillis())
}

// version 1 → 2 (스키마 변경: storeId/serverCallId Long → String)
// fallbackToDestructiveMigration 설정되어 있어 자동으로 DB 재생성됨
@Database(entities = [RecordingEntity::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}