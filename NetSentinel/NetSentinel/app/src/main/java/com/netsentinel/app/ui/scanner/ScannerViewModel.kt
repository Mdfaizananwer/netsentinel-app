package com.netsentinel.app.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.data.network.NetworkUtils
import com.netsentinel.app.data.scanner.HostResult
import com.netsentinel.app.data.scanner.NetworkScanner
import com.netsentinel.app.data.scanner.ScanEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannerUiState(
    val localIp: String? = null,
    val prefixLength: Int = 24,
    val startIp: String = "",
    val endIp: String = "",
    val isScanning: Boolean = false,
    val scanned: Int = 0,
    val total: Int = 0,
    val currentIp: String = "",
    val results: List<HostResult> = emptyList(),
    val errorMessage: String? = null,
    val lastScanDurationMillis: Long? = null,
)

class ScannerViewModel(private val app: NetSentinelApp) : ViewModel() {

    private val _ui = MutableStateFlow(ScannerUiState())
    val ui: StateFlow<ScannerUiState> = _ui.asStateFlow()
    private val scanner = NetworkScanner()
    private var scanJob: Job? = null

    init {
        val snapshot = app.wifiRepository.snapshotOnce()
        val prefix = app.wifiRepository.currentPrefixLengthOrDefault()
        val ip = snapshot.localIpv4
        if (ip != null) {
            val subnet = NetworkUtils.deriveSubnet(ip, prefix)
            // Default to a sensible bounded slice (first 254 hosts of the subnet) rather than
            // the whole subnet, keeping the default scan fast and within the safety cap.
            val defaultStart = NetworkUtils.longToIp(NetworkUtils.ipToLong(subnet.networkAddress) + 1)
            val defaultEndLong = minOf(
                NetworkUtils.ipToLong(subnet.networkAddress) + 254,
                NetworkUtils.ipToLong(subnet.broadcastAddress) - 1
            )
            _ui.update {
                it.copy(localIp = ip, prefixLength = prefix, startIp = defaultStart, endIp = NetworkUtils.longToIp(defaultEndLong))
            }
        } else {
            _ui.update { it.copy(errorMessage = "No local IPv4 address available — connect to Wi-Fi first.") }
        }
    }

    fun updateStartIp(value: String) = _ui.update { it.copy(startIp = value) }
    fun updateEndIp(value: String) = _ui.update { it.copy(endIp = value) }

    fun startScan() {
        val state = _ui.value
        val localIp = state.localIp ?: return
        val validated = NetworkUtils.validateBoundedRange(localIp, state.prefixLength, state.startIp, state.endIp)
        validated.onFailure { e ->
            _ui.update { it.copy(errorMessage = e.message) }
            return
        }
        val range = validated.getOrThrow()
        _ui.update { it.copy(isScanning = true, results = emptyList(), scanned = 0, total = 0, errorMessage = null) }

        scanJob = viewModelScope.launch {
            val seenIps = mutableListOf<String>()
            scanner.scan(range).collect { event ->
                when (event) {
                    is ScanEvent.Progress -> _ui.update { it.copy(scanned = event.scanned, total = event.total, currentIp = event.currentIp) }
                    is ScanEvent.HostFound -> {
                        seenIps.add(event.host.ipAddress)
                        _ui.update { it.copy(results = it.results + event.host) }
                        app.deviceRepository.recordScanResult(event.host)
                    }
                    is ScanEvent.Completed -> {
                        _ui.update { it.copy(isScanning = false, lastScanDurationMillis = event.durationMillis) }
                        app.deviceRepository.markOfflineExcept(seenIps)
                    }
                    is ScanEvent.Cancelled -> _ui.update { it.copy(isScanning = false) }
                    is ScanEvent.Failed -> _ui.update { it.copy(isScanning = false, errorMessage = event.reason) }
                }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _ui.update { it.copy(isScanning = false) }
    }
}
