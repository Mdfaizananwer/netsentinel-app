package com.netsentinel.app.data.repository

import android.content.Context
import com.netsentinel.app.data.db.NetworkEventDao
import com.netsentinel.app.mitm.ArpMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay

class MitmMonitorRepository(context: Context, private val eventDao: NetworkEventDao) {
    private val monitor = ArpMonitor(context, eventDao)

    fun observeEvents() = eventDao.observeAll()

    /** Runs the defensive check on an interval while collected (e.g. while the lab screen is open). */
    fun watch(intervalMillis: Long = 10_000L): Flow<Unit> = flow {
        while (true) {
            monitor.checkOnce()
            emit(Unit)
            delay(intervalMillis)
        }
    }

    suspend fun checkNow() = monitor.checkOnce()

    suspend fun clearEvents() = eventDao.deleteAll()
}
