package com.rarible.protocol.erc20.listener.scanner

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.common.EventTimeMarks
import com.rarible.core.test.data.randomBoolean
import com.rarible.protocol.erc20.core.randomReversedEthereumLogRecord
import com.rarible.protocol.erc20.core.repository.data.randomBalance
import com.rarible.protocol.erc20.core.repository.data.randomErc20IncomeTransfer
import com.rarible.protocol.erc20.core.repository.data.randomErc20TokenApproval
import com.rarible.protocol.erc20.listener.service.Erc20BalanceReduceService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class FullReduceReconciliationListenerTest {
    private val reducer = mockk<Erc20BalanceReduceService>()
    private val listener = FullReduceReconciliationListener(reducer)

    @Test
    fun reduce() = runBlocking<Unit> {
        val balanceEvent1 = randomErc20IncomeTransfer()
        val balanceEvent2 = randomErc20IncomeTransfer()
        val approval = randomErc20TokenApproval()
        val event1 = randomReversedEthereumLogRecord(balanceEvent1)
        val event2 = randomReversedEthereumLogRecord(balanceEvent1)
        val event3 = randomReversedEthereumLogRecord(approval)
        val event4 = randomReversedEthereumLogRecord(balanceEvent2)

        coEvery { reducer.update(balanceEvent1.token, balanceEvent1.owner) } returns randomBalance()
        coEvery { reducer.update(balanceEvent2.token, balanceEvent2.owner) } returns randomBalance()
        coEvery { reducer.update(approval.token, approval.owner) } returns randomBalance()

        listener.onLogRecordEvent(
            groupId = "test",
            logRecordEvents = listOf(event1, event2, event3, event4).map { LogRecordEvent(it, randomBoolean(), EventTimeMarks("test")) }
        )
        coVerify(exactly = 1) {
            reducer.update(balanceEvent1.token, balanceEvent1.owner)
            reducer.update(balanceEvent2.token, balanceEvent2.owner)
            reducer.update(approval.token, approval.owner)
        }
    }
}
