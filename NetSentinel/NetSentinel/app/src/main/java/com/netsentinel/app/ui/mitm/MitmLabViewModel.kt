package com.netsentinel.app.ui.mitm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.data.db.NetworkEventEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MitmLabViewModel(private val app: NetSentinelApp) : ViewModel() {
    val events: StateFlow<List<NetworkEventEntity>> = app.mitmMonitorRepository.observeEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            app.mitmMonitorRepository.watch().collect { /* side-effecting writes happen inside watch() */ }
        }
    }

    fun checkNow() = viewModelScope.launch { app.mitmMonitorRepository.checkNow() }
    fun clearEvents() = viewModelScope.launch { app.mitmMonitorRepository.clearEvents() }
}
