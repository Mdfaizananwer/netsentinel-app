package com.netsentinel.app.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.data.db.DeviceEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortMode { LAST_SEEN, IP_ADDRESS, HOSTNAME }

class InventoryViewModel(private val app: NetSentinelApp) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _sortMode = MutableStateFlow(SortMode.LAST_SEEN)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    val devices: StateFlow<List<DeviceEntity>> = combine(
        app.deviceRepository.observeAll(), _query, _sortMode
    ) { list, query, sort ->
        val filtered = if (query.isBlank()) list else list.filter {
            it.ipAddress.contains(query, true) ||
                it.hostname?.contains(query, true) == true ||
                it.nickname?.contains(query, true) == true ||
                it.macAddress?.contains(query, true) == true
        }
        when (sort) {
            SortMode.LAST_SEEN -> filtered.sortedByDescending { it.lastSeenMillis }
            SortMode.IP_ADDRESS -> filtered.sortedBy { com.netsentinel.app.data.network.NetworkUtils.ipToLong(it.ipAddress) }
            SortMode.HOSTNAME -> filtered.sortedBy { it.hostname ?: it.ipAddress }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setSortMode(mode: SortMode) { _sortMode.value = mode }

    suspend fun exportCsv(): String = app.deviceRepository.exportCsv(devices.value)
    suspend fun exportJson(): String = app.deviceRepository.exportJson(devices.value)

    fun deleteDevice(ip: String) = viewModelScope.launch { app.deviceRepository.delete(ip) }
}
