package com.netsentinel.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.data.network.WifiSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(private val app: NetSentinelApp) : ViewModel() {

    private val _snapshot = MutableStateFlow<WifiSnapshot?>(null)
    val snapshot: StateFlow<WifiSnapshot?> = _snapshot.asStateFlow()

    val deviceCount = app.deviceRepository.observeAll()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _snapshot.value = app.wifiRepository.snapshotOnce()
        }
    }
}
