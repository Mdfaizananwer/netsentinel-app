package com.netsentinel.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "capture_sessions")
data class CaptureSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val totalPackets: Long,
    val totalBytes: Long,
    val tcpCount: Long,
    val udpCount: Long,
    val dnsCount: Long,
    val otherCount: Long,
)

/**
 * Sanitized packet *metadata* only — see PacketParser for what is and isn't captured.
 * No payload bytes, headers or any field that could carry credentials/cookies are stored.
 */
@Entity(tableName = "packet_samples")
data class PacketSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestampMillis: Long,
    val sourceIp: String,
    val destIp: String,
    val protocol: String, // TCP, UDP, ICMP, OTHER
    val sourcePort: Int?,
    val destPort: Int?,
    val sizeBytes: Int,
)

@Dao
interface CaptureSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: CaptureSessionEntity): Long

    @Query("SELECT * FROM capture_sessions ORDER BY startedAtMillis DESC")
    fun observeAll(): Flow<List<CaptureSessionEntity>>

    @Query("SELECT * FROM capture_sessions WHERE id = :id")
    suspend fun getById(id: Long): CaptureSessionEntity?

    @Query("DELETE FROM capture_sessions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PacketSampleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(samples: List<PacketSampleEntity>)

    @Query("SELECT * FROM packet_samples WHERE sessionId = :sessionId ORDER BY timestampMillis ASC")
    suspend fun getForSession(sessionId: Long): List<PacketSampleEntity>

    @Query("SELECT * FROM packet_samples WHERE sessionId = :sessionId ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeRecentForSession(sessionId: Long, limit: Int = 200): Flow<List<PacketSampleEntity>>

    @Query("DELETE FROM packet_samples WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}
