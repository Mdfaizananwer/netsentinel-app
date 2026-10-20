package com.netsentinel.app.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.capture.CaptureUiState
import com.netsentinel.app.capture.ParsedPacket
import com.netsentinel.app.capture.Protocol
import com.netsentinel.app.capture.ProtocolFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PacketCaptureViewModel(private val app: NetSentinelApp) : ViewModel() {

    val state: StateFlow<CaptureUiState> = app.captureRepository.state

    private val _filter = MutableStateFlow(ProtocolFilter.ALL)
    val filter: StateFlow<ProtocolFilter> = _filter.asStateFlow()

    private val _packets = MutableStateFlow<List<ParsedPacket>>(emptyList())
    val packets: StateFlow<List<ParsedPacket>> = _packets.asStateFlow()

    private var sessionStartedAt: Long? = null

    /** Returns null if VPN consent is already granted; otherwise an Intent to launch for consent. */
    fun vpnConsentIntentOrNull() = app.captureRepository.prepareIntent()

    fun start() {
        sessionStartedAt = System.currentTimeMillis()
        _packets.value = emptyList()
        app.captureRepository.start()
        pollPackets()
    }

    private fun pollPackets() {
        viewModelScope.launch {
            com.netsentinel.app.capture.PacketCaptureVpnService.packetEvents.collect { latest ->
                latest ?: return@collect
                _packets.value = (_packets.value + latest).takeLast(300)
            }
        }
    }

    fun pause() = app.captureRepository.pause()
    fun resume() = app.captureRepository.resume()

    fun stop() {
        app.captureRepository.stop()
        viewModelScope.launch {
            val finalState = state.value
            if (finalState is CaptureUiState.Running || finalState is CaptureUiState.Paused) {
                val stats = (finalState as? CaptureUiState.Running)?.stats
                val startedAt = sessionStartedAt ?: System.currentTimeMillis()
                if (stats != null) {
                    val sessionId = app.captureRepository.persistSession(startedAt, System.currentTimeMillis(), stats)
                    app.captureRepository.persistPacketBatch(sessionId, _packets.value)
                }
            }
        }
    }

    fun setFilter(f: ProtocolFilter) { _filter.value = f }

    fun filteredPackets(): List<ParsedPacket> {
        val f = _filter.value
        return when (f) {
            ProtocolFilter.ALL -> _packets.value
            ProtocolFilter.TCP -> _packets.value.filter { it.protocol == Protocol.TCP }
            ProtocolFilter.UDP -> _packets.value.filter { it.protocol == Protocol.UDP }
            ProtocolFilter.DNS -> _packets.value.filter { it.dnsQueryName != null }
            ProtocolFilter.OTHER -> _packets.value.filter { it.protocol == Protocol.OTHER || it.protocol == Protocol.ICMP }
        }
    }
}
