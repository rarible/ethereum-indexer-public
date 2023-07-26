package com.rarible.protocol.erc20.core.service.reduce.reversed

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.randomDepositEvent
import com.rarible.protocol.erc20.core.randomIncomeTransferEvent
import com.rarible.protocol.erc20.core.randomOutcomeTransferEvent
import com.rarible.protocol.erc20.core.randomTokenApprovalEvent
import com.rarible.protocol.erc20.core.randomWithdrawalEvent
import com.rarible.protocol.erc20.core.repository.data.randomBalance
import com.rarible.protocol.erc20.core.service.reduce.forward.ForwardValueErc20BalanceReducer
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class ReversedValueErc20BalanceReducerTest {

    private val reversedValueErc20BalanceReducer = ReversedValueErc20BalanceReducer(
        ForwardValueErc20BalanceReducer()
    )

    @Test
    fun `should calculate value on income transfer event`() = runBlocking<Unit> {

        val event = randomIncomeTransferEvent().copy(value = EthUInt256.of(2))
        val balance = randomBalance(balance = EthUInt256.of(7))

        val reducedItem = reversedValueErc20BalanceReducer.reduce(balance, event)
        Assertions.assertThat(reducedItem.balance).isEqualTo(EthUInt256.of(5))
    }

    @Test
    fun `should calculate value on outcome transfer event`() = runBlocking<Unit> {

        val event = randomOutcomeTransferEvent().copy(value = EthUInt256.of(7))
        val balance = randomBalance(balance = EthUInt256.of(9))

        val reducedItem = reversedValueErc20BalanceReducer.reduce(balance, event)
        Assertions.assertThat(reducedItem.balance).isEqualTo(EthUInt256.of(16))
    }

    @Test
    fun `should calculate value on withdrawal event`() = runBlocking<Unit> {

        val event = randomWithdrawalEvent().copy(value = EthUInt256.of(3))
        val balance = randomBalance(balance = EthUInt256.of(10))

        val reducedItem = reversedValueErc20BalanceReducer.reduce(balance, event)
        Assertions.assertThat(reducedItem.balance).isEqualTo(EthUInt256.of(13))
    }

    @Test
    fun `should calculate value on deposit event`() = runBlocking<Unit> {

        val event = randomDepositEvent().copy(value = EthUInt256.of(3))
        val balance = randomBalance(balance = EthUInt256.of(10))

        val reducedItem = reversedValueErc20BalanceReducer.reduce(balance, event)
        Assertions.assertThat(reducedItem.balance).isEqualTo(EthUInt256.of(7))
    }

    @Test
    fun `should calculate value on token approval event`() = runBlocking<Unit> {

        val event = randomTokenApprovalEvent().copy(value = EthUInt256.of(3))
        val balance = randomBalance(balance = EthUInt256.of(10))

        val reducedItem = reversedValueErc20BalanceReducer.reduce(balance, event)
        Assertions.assertThat(reducedItem.balance).isEqualTo(EthUInt256.of(10))
    }
}
