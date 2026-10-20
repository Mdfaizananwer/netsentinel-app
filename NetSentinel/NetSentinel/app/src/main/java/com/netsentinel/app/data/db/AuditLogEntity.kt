package com.netsentinel.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Audit trail for the companion-agent device-control feature (pairing, commands sent/received). */
@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val peerId: String,
    val action: String, // PAIR_REQUEST, PAIR_APPROVED, PAIR_REVOKED, COMMAND_SENT, COMMAND_RESULT
    val detail: String,
)

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insert(entry: AuditLogEntity)

    @Query("SELECT * FROM audit_log ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<AuditLogEntity>>
}
