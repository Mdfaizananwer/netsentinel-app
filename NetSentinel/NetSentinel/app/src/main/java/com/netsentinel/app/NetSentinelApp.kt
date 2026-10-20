package com.netsentinel.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.netsentinel.app.data.db.AppDatabase
import com.netsentinel.app.data.repository.CaptureRepository
import com.netsentinel.app.data.repository.DeviceRepository
import com.netsentinel.app.data.repository.MitmMonitorRepository
import com.netsentinel.app.data.repository.WifiRepository

/**
 * Application-level manual service locator.
 *
 * The project intentionally avoids a DI framework (Hilt/Dagger) to keep the build graph
 * small and easy to reason about for a security-review-oriented codebase; every dependency
 * is constructed explicitly, once, here.
 */
class NetSentinelApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var wifiRepository: WifiRepository
        private set
    lateinit var deviceRepository: DeviceRepository
        private set
    lateinit var captureRepository: CaptureRepository
        private set
    lateinit var mitmMonitorRepository: MitmMonitorRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.build(this)
        wifiRepository = WifiRepository(this)
        deviceRepository = DeviceRepository(database.deviceDao())
        captureRepository = CaptureRepository(this, database.captureSessionDao(), database.packetSampleDao())
        mitmMonitorRepository = MitmMonitorRepository(this, database.networkEventDao())
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CAPTURE_CHANNEL_ID,
                "Packet Capture Lab",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Persistent status while the on-device capture VPN is running." }
        )
    }

    companion object {
        const val CAPTURE_CHANNEL_ID = "capture_lab"
    }
}
