package com.netsentinel.app.capture

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.netsentinel.app.MainActivity
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real, working implementation of the "Packet Capture Lab" using Android's VpnService API.
 *
 * SCOPE AND HONEST LIMITATIONS (also surfaced in the UI, per the project's "don't claim more
 * than is implemented" requirement):
 *  - This establishes a genuine local VPN (TUN) interface on-device and reads real IPv4 packets
 *    that the OS routes through it once the user grants the system VPN permission and enables
 *    capture.
 *  - It is METADATA-ONLY and OBSERVE-ONLY: it parses IP/TCP/UDP headers (addresses, ports,
 *    protocol, size, and — for UDP/53 — the plain-text DNS query name) and never reads
 *    application payload bytes, so it structurally cannot capture passwords, cookies, tokens,
 *    or message contents, and it never touches TLS.
 *  - This build does NOT forward captured packets back out to the real network (that would
 *    require implementing a full userspace NAT/TCP-relay stack, which is out of scope for an
 *    auditable, correctness-focused lab build). Practically: while capture is ON, other apps'
 *    traffic that gets routed through this VPN will be paused/dropped, not proxied through to
 *    the internet. This is disclosed prominently before the user starts a capture.
 *  - Per VpnService's design, this interface only ever sees traffic the OS routes into *this*
 *    app's TUN device — never other devices' LAN traffic, and never traffic Android chooses to
 *    keep outside the VPN (e.g. other VPN apps, or on some OEMs, certain system traffic).
 */
class PacketCaptureVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val running = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopCapture()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                paused.set(true)
                _state.update { CaptureUiState.Paused }
                return START_STICKY
            }
            ACTION_RESUME -> {
                paused.set(false)
                return START_STICKY
            }
            else -> startCapture()
        }
        return START_STICKY
    }

    private fun startCapture() {
        if (running.get()) return
        val builder = Builder()
            .setSession("NetSentinel Capture Lab")
            .addAddress(TUN_ADDRESS, 32)
            .addRoute("0.0.0.0", 0)
            .setMtu(MTU)
            .setBlocking(false)

        vpnInterface = try {
            builder.establish()
        } catch (e: Exception) {
            _state.update { CaptureUiState.Error("Failed to establish VPN interface: ${e.message}") }
            null
        }

        val fd = vpnInterface ?: return
        running.set(true)
        paused.set(false)
        val startedAt = System.currentTimeMillis()
        statsAccumulator = CaptureStats()
        _state.update { CaptureUiState.Running(statsAccumulator, startedAt) }
        startForeground(NOTIFICATION_ID, buildNotification())

        serviceScope.launch { readLoop(fd, startedAt) }
    }

    private fun readLoop(pfd: ParcelFileDescriptor, startedAt: Long) {
        val input = FileInputStream(pfd.fileDescriptor)
        val buffer = ByteArray(MTU)
        try {
            while (running.get()) {
                if (paused.get()) {
                    Thread.sleep(150)
                    continue
                }
                val length = try {
                    input.read(buffer)
                } catch (e: IOException) {
                    if (running.get()) { /* interface was torn down from under us */ }
                    break
                }
                if (length <= 0) continue

                val packet = PacketParser.parse(buffer, length, System.currentTimeMillis()) ?: continue
                onPacketCaptured(packet)
            }
        } finally {
            input.close()
        }
    }

    private var statsAccumulator = CaptureStats()
    private val recentPackets = ArrayDeque<ParsedPacket>()

    private fun onPacketCaptured(packet: ParsedPacket) {
        statsAccumulator = statsAccumulator.copy(
            totalPackets = statsAccumulator.totalPackets + 1,
            totalBytes = statsAccumulator.totalBytes + packet.totalLengthBytes,
            tcpCount = statsAccumulator.tcpCount + if (packet.protocol == Protocol.TCP) 1 else 0,
            udpCount = statsAccumulator.udpCount + if (packet.protocol == Protocol.UDP) 1 else 0,
            dnsCount = statsAccumulator.dnsCount + if (packet.dnsQueryName != null) 1 else 0,
            otherCount = statsAccumulator.otherCount + if (packet.protocol == Protocol.OTHER || packet.protocol == Protocol.ICMP) 1 else 0,
        )
        synchronized(recentPackets) {
            recentPackets.addLast(packet)
            if (recentPackets.size > MAX_BUFFERED_PACKETS) recentPackets.removeFirst()
        }
        _state.update { CaptureUiState.Running(statsAccumulator, (it as? CaptureUiState.Running)?.startedAtMillis ?: System.currentTimeMillis()) }
        _packetEvents.update { packet }
    }

    fun drainBufferedPackets(): List<ParsedPacket> = synchronized(recentPackets) {
        val copy = recentPackets.toList()
        recentPackets.clear()
        copy
    }

    private fun stopCapture() {
        running.set(false)
        val finalStats = statsAccumulator
        _state.update { CaptureUiState.Stopped(finalStats, null) }
        try {
            vpnInterface?.close()
        } catch (_: IOException) {
            // Already closed/torn down — nothing actionable.
        }
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        running.set(false)
        try { vpnInterface?.close() } catch (_: IOException) { }
        super.onDestroy()
    }

    override fun onRevoke() {
        // User revoked VPN permission from system settings — tear down cleanly.
        stopCapture()
        super.onRevoke()
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 0, Intent(this, PacketCaptureVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, NetSentinelApp.CAPTURE_CHANNEL_ID)
            .setContentTitle(getString(R.string.vpn_capture_notification_title))
            .setContentText(getString(R.string.vpn_capture_notification_text))
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, "Stop capture", stopIntent)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.netsentinel.app.capture.STOP"
        const val ACTION_PAUSE = "com.netsentinel.app.capture.PAUSE"
        const val ACTION_RESUME = "com.netsentinel.app.capture.RESUME"
        private const val NOTIFICATION_ID = 42
        private const val TUN_ADDRESS = "10.233.0.1" // RFC1918-adjacent address local to the TUN interface only
        private const val MTU = 1500
        private const val MAX_BUFFERED_PACKETS = 500

        private val _state = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
        val state: StateFlow<CaptureUiState> = _state.asStateFlow()

        private val _packetEvents = MutableStateFlow<ParsedPacket?>(null)
        val packetEvents: StateFlow<ParsedPacket?> = _packetEvents.asStateFlow()
    }
}
