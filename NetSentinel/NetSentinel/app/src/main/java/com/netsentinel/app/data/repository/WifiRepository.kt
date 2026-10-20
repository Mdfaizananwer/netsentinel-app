package com.netsentinel.app.data.repository

import android.content.Context
import com.netsentinel.app.data.network.WifiInfoProvider
import com.netsentinel.app.data.network.WifiSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WifiRepository(context: Context) {
    private val provider = WifiInfoProvider(context)

    fun snapshotOnce(): WifiSnapshot = provider.currentSnapshot()

    fun currentPrefixLengthOrDefault(): Int = provider.currentPrefixLengthOrDefault()

    /** Polls system Wi-Fi state every [intervalMillis]; this is local-only and cheap (no I/O). */
    fun observeSnapshot(intervalMillis: Long = 3000L): Flow<WifiSnapshot> = flow {
        while (true) {
            emit(provider.currentSnapshot())
            delay(intervalMillis)
        }
    }
}
