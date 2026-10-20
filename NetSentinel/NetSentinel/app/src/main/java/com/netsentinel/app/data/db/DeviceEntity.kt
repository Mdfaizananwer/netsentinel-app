package com.netsentinel.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val ipAddress: String,
    val hostname: String?,
    val macAddress: String?,
    val nickname: String?,
    val discoveryMethod: String,
    val firstSeenMillis: Long,
    val lastSeenMillis: Long,
    val lastResponseTimeMillis: Long,
    val openPortsCsv: String, // stored as "80,443" to keep the entity Room-simple without a TypeConverter
    val isOnline: Boolean,
)
