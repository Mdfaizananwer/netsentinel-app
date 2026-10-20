package com.netsentinel.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Defensive-monitor log: changes to gateway IP/MAC or DNS servers that this device itself observed. */
@Entity(tableName = "network_events")
data class NetworkEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val eventType: String, // GATEWAY_IP_CHANGED, GATEWAY_MAC_CHANGED, DNS_CHANGED, ARP_TABLE_CHANGED
    val previousValue: String?,
    val newValue: String?,
    val severity: String, // INFO, WARNING
)

@Dao
interface NetworkEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: NetworkEventEntity)

    @Query("SELECT * FROM network_events ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<NetworkEventEntity>>

    @Query("DELETE FROM network_events")
    suspend fun deleteAll()
}
