package com.netsentinel.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DeviceEntity::class,
        CaptureSessionEntity::class,
        PacketSampleEntity::class,
        NetworkEventEntity::class,
        AuditLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun captureSessionDao(): CaptureSessionDao
    abstract fun packetSampleDao(): PacketSampleDao
    abstract fun networkEventDao(): NetworkEventDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        /** No remote backend: database lives purely on-device at the default private app path. */
        fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "netsentinel.db"
        ).build()
    }
}
