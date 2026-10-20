package com.netsentinel.app.ui.wifi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.data.network.WifiSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class WifiAnalyzerViewModel(app: NetSentinelApp) : ViewModel() {
    val snapshot: StateFlow<WifiSnapshot?> = app.wifiRepository.observeSnapshot()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
