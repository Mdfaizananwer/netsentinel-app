package com.netsentinel.app.control

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Placeholder Android Service entry point for a future real companion-agent transport
 * (see the scope note in CompanionAgentProtocol.kt). It is declared in the manifest as
 * non-exported and is never started automatically — the Device Control screen only drives
 * the in-process [CompanionAgentManager] with the loopback transport today.
 */
class CompanionAgentService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
