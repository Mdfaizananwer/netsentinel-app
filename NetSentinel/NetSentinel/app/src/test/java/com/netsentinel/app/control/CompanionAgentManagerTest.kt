package com.netsentinel.app.control

import com.google.common.truth.Truth.assertThat
import com.netsentinel.app.data.db.AuditLogDao
import com.netsentinel.app.data.db.AuditLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Test

private class FakeAuditLogDao : AuditLogDao {
    private val entries = MutableStateFlow<List<AuditLogEntity>>(emptyList())
    override suspend fun insert(entry: AuditLogEntity) {
        entries.update { it + entry.copy(id = (it.size + 1).toLong()) }
    }
    override fun observeAll(): Flow<List<AuditLogEntity>> = entries.asStateFlow()
    fun snapshot() = entries.value
}

class CompanionAgentManagerTest {

    @Test
    fun `commands are rejected until pairing is actively approved`() = runBlocking {
        val dao = FakeAuditLogDao()
        val manager = CompanionAgentManager(LoopbackCompanionAgentTransport(), dao)

        manager.requestPairing("peer-1", "Test Device")
        val rejected = manager.sendCommand("peer-1", AllowlistedCommand.PING)
        assertThat(rejected).isInstanceOf(CommandResult.Rejected::class.java)

        manager.approvePairing("peer-1")
        val accepted = manager.sendCommand("peer-1", AllowlistedCommand.PING)
        assertThat(accepted).isInstanceOf(CommandResult.Success::class.java)
        assertThat((accepted as CommandResult.Success).payload).isEqualTo("pong")
    }

    @Test
    fun `revoked pairing rejects further commands`() = runBlocking {
        val dao = FakeAuditLogDao()
        val manager = CompanionAgentManager(LoopbackCompanionAgentTransport(), dao)

        manager.requestPairing("peer-2", "Another Device")
        manager.approvePairing("peer-2")
        manager.revokePairing("peer-2")

        val result = manager.sendCommand("peer-2", AllowlistedCommand.REQUEST_STATUS)
        assertThat(result).isInstanceOf(CommandResult.Rejected::class.java)
    }

    @Test
    fun `every state transition writes an audit entry`() = runBlocking {
        val dao = FakeAuditLogDao()
        val manager = CompanionAgentManager(LoopbackCompanionAgentTransport(), dao)

        manager.requestPairing("peer-3", "Audited Device")
        manager.approvePairing("peer-3")
        manager.sendCommand("peer-3", AllowlistedCommand.PING)
        manager.revokePairing("peer-3")

        val actions = dao.snapshot().map { it.action }
        assertThat(actions).containsAtLeast("PAIR_REQUEST", "PAIR_APPROVED", "COMMAND_SENT", "COMMAND_RESULT", "PAIR_REVOKED")
    }

    @Test
    fun `unknown peer is rejected without throwing`() = runBlocking {
        val dao = FakeAuditLogDao()
        val manager = CompanionAgentManager(LoopbackCompanionAgentTransport(), dao)
        val result = manager.sendCommand("never-paired", AllowlistedCommand.PING)
        assertThat(result).isInstanceOf(CommandResult.Rejected::class.java)
    }
}
