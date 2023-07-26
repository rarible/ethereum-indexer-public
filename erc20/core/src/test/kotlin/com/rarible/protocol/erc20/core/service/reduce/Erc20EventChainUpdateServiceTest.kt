package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.erc20.core.repository.data.randomBalanceId
import com.rarible.protocol.erc20.core.repository.data.randomErc20OutcomeTransfer
import com.rarible.protocol.erc20.core.repository.data.randomErc20TokenApproval
import com.rarible.protocol.erc20.core.repository.data.randomLogEvent
import com.rarible.protocol.erc20.core.service.Erc20BalanceService
import com.rarible.protocol.erc20.core.service.Erc20AllowanceService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class Erc20EventChainUpdateServiceTest {
    private val converter = Erc20EventConverter()
    private val delegate = mockk<Erc20EventReduceService>()
    private val balanceService = mockk<Erc20BalanceService>()
    private val allowanceService = mockk<Erc20AllowanceService>()

    private val service = Erc20EventChainUpdateService(converter, delegate, balanceService, allowanceService)

    @Test
    fun `update via chain - ok`() = runBlocking<Unit> {
        val balanceId1 = randomBalanceId()
        val balanceId2 = randomBalanceId()

        val events = listOf(
            randomLogEvent(randomErc20OutcomeTransfer(balanceId1.token, balanceId1.owner)),
            randomLogEvent(randomErc20OutcomeTransfer(balanceId2.token, balanceId2.owner)),
        ).map { LogRecordEvent(it, false, EventTimeMarks("test", emptyList())) }

        coEvery { balanceService.onChainUpdate(balanceId1, any()) } returns mockk()
        coEvery { balanceService.onChainUpdate(balanceId2, any()) } returns mockk()

        service.onEntityEvents(events)

        coVerify { balanceService.onChainUpdate(balanceId1, any()) }
        coVerify { balanceService.onChainUpdate(balanceId2, any()) }
        coVerify(exactly = 0) { delegate.onEntityEvents(any()) }
    }

    @Test
    fun `call delegate`() = runBlocking<Unit> {
        val balanceId = randomBalanceId()

        val events = listOf(
            randomLogEvent(randomErc20OutcomeTransfer(balanceId.token, balanceId.owner)),
        ).map { LogRecordEvent(it, false, EventTimeMarks("test", emptyList())) }

        coEvery { balanceService.onChainUpdate(balanceId, any()) } returns null
        coEvery { delegate.onEntityEvents(events) } returns Unit

        service.onEntityEvents(events)

        coVerify { balanceService.onChainUpdate(balanceId, any()) }
        coVerify { delegate.onEntityEvents(events) }
    }

    @Test
    fun `erc20 approval`() = runBlocking<Unit> {
        val balanceId = randomBalanceId()

        val events = listOf(
            randomLogEvent(randomErc20TokenApproval(balanceId.token, balanceId.owner)),
        ).map { LogRecordEvent(it, false, EventTimeMarks("test", emptyList())) }

        coEvery { balanceService.onChainUpdate(balanceId, any()) } returns mockk()
        coEvery { allowanceService.onChainUpdate(balanceId, any()) } returns Unit

        service.onEntityEvents(events)

        coVerify { balanceService.onChainUpdate(balanceId, any()) }
        coVerify { allowanceService.onChainUpdate(balanceId, any()) }
        coVerify(exactly = 0) { delegate.onEntityEvents(any()) }
    }
}
