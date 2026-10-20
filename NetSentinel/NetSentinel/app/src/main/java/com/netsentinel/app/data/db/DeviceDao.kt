package com.netsentinel.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY lastSeenMillis DESC")
    fun observeAll(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE ipAddress = :ip")
    suspend fun getByIp(ip: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: DeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(devices: List<DeviceEntity>)

    @Update
    suspend fun update(device: DeviceEntity)

    @Query("UPDATE devices SET nickname = :nickname WHERE ipAddress = :ip")
    suspend fun setNickname(ip: String, nickname: String?)

    @Query("UPDATE devices SET isOnline = 0 WHERE ipAddress NOT IN (:seenIps)")
    suspend fun markOfflineExcept(seenIps: List<String>)

    @Query("DELETE FROM devices WHERE ipAddress = :ip")
    suspend fun delete(ip: String)

    @Query("DELETE FROM devices")
    suspend fun deleteAll()
}
