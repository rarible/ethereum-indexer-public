package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.protocol.erc20.core.randomIncomeTransferEvent
import com.rarible.protocol.erc20.core.repository.data.randomBalance
import com.rarible.protocol.erc20.core.service.reduce.forward.ForwardChainErc20BalanceReducer
import com.rarible.protocol.erc20.core.service.reduce.reversed.ReversedChainErc20BalanceReducer
import com.rarible.protocol.erc20.core.service.reduce.reversed.RevertBalanceCompactEventsReducer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class EventStatusErc20BalanceReducerTest {

    private val forwardChainErc20BalanceReducer = mockk<ForwardChainErc20BalanceReducer>()
    private val reversedChainErc20BalanceReducer = mockk<ReversedChainErc20BalanceReducer>()

    private val eventStatusErc20BalanceReducer = EventStatusErc20BalanceReducer(
        forwardChainErc20BalanceReducer = forwardChainErc20BalanceReducer,
        reversedChainErc20BalanceReducer = reversedChainErc20BalanceReducer,
        revertBalanceCompactEventsReducer = RevertBalanceCompactEventsReducer()
    )

    @Test
    fun `should handle confirm event`() = runBlocking<Unit> {
        val event = randomIncomeTransferEvent()
            .let { it.copy(log = it.log.copy(status = EthereumBlockStatus.CONFIRMED)) }
        val entity = randomBalance()

        coEvery { forwardChainErc20BalanceReducer.reduce(entity, event) } returns entity
        val reducedItem = eventStatusErc20BalanceReducer.reduce(entity, event)
        Assertions.assertThat(reducedItem).isEqualTo(entity)

        coVerify { forwardChainErc20BalanceReducer.reduce(entity, event) }
        coVerify(exactly = 0) { reversedChainErc20BalanceReducer.reduce(any(), any()) }
    }

    @Test
    fun `should handle revert event`() = runBlocking<Unit> {
        val event = randomIncomeTransferEvent()
            .let { it.copy(log = it.log.copy(status = EthereumBlockStatus.REVERTED)) }
        val entity = randomBalance()

        coEvery { reversedChainErc20BalanceReducer.reduce(entity, event) } returns entity
        val reducedItem = eventStatusErc20BalanceReducer.reduce(entity, event)
        Assertions.assertThat(reducedItem).isEqualTo(entity)

        coVerify { reversedChainErc20BalanceReducer.reduce(entity, event) }
        coVerify(exactly = 0) { forwardChainErc20BalanceReducer.reduce(any(), any()) }
    }

    @Test
    fun `should handle pending event`() = runBlocking<Unit> {
        val event = randomIncomeTransferEvent().let { it.copy(log = it.log.copy(status = EthereumBlockStatus.PENDING)) }
        val entity = randomBalance()

        val reducedItem = eventStatusErc20BalanceReducer.reduce(entity, event)
        Assertions.assertThat(reducedItem).isEqualTo(entity)

        coVerify(exactly = 0) { forwardChainErc20BalanceReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { reversedChainErc20BalanceReducer.reduce(any(), any()) }
    }

    @Test
    fun `should handle inactive event`() = runBlocking<Unit> {
        val event = randomIncomeTransferEvent()
            .let { it.copy(log = it.log.copy(status = EthereumBlockStatus.INACTIVE)) }
        val entity = randomBalance()

        val reducedItem = eventStatusErc20BalanceReducer.reduce(entity, event)
        Assertions.assertThat(reducedItem).isEqualTo(entity)

        coVerify(exactly = 0) { forwardChainErc20BalanceReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { reversedChainErc20BalanceReducer.reduce(any(), any()) }
    }

    @Test
    fun `should handle drop event`() = runBlocking<Unit> {
        val event = randomIncomeTransferEvent()
            .let { it.copy(log = it.log.copy(status = EthereumBlockStatus.DROPPED)) }
        val entity = randomBalance()

        val reducedItem = eventStatusErc20BalanceReducer.reduce(entity, event)
        Assertions.assertThat(reducedItem).isEqualTo(entity)

        coVerify(exactly = 0) { forwardChainErc20BalanceReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { reversedChainErc20BalanceReducer.reduce(any(), any()) }
    }
}
