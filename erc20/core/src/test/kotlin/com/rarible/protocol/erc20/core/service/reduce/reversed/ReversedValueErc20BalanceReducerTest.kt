package com.rarible.protocol.erc20.core.service.reduce.reversed

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.createRandomDepositEvent
import com.rarible.protocol.erc20.core.createRandomIncomeTransferEvent
import com.rarible.protocol.erc20.core.createRandomOutcomeTransferEvent
import com.rarible.protocol.erc20.core.createRandomTokenApprovalEvent
import com.rarible.protocol.erc20.core.createRandomWithdrawalEvent
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

        val event = createRandomIncomeTransferEvent().copy(value = EthUInt256.of(2))
        val balance = randomBalance(balance = EthUInt256.of(7))

        val reducedItem = reversedValueErc20BalanceReducer.reduce(balance, event)
        Assertions.assertThat(reducedItem.balance).isEqualTo(EthUInt256.of(5))
    }

    @Test
    fun `should calculate value on outcome transfer event`() = runBlocking<Unit> {

        val event = createRandomOutcomeTransferEvent().copy(value = EthUInt256.of(7))
        val balance = randomBalance(balance = EthUInt256.of(9))

        val reducedItem = reversedValueErc20BalanceReducer.reduce(balance, event)
        Assertions.assertThat(reducedItem.balance).isEqualTo(EthUInt256.of(16))
    }

    @Test
    fun `should calculate value on withdrawal event`() = runBlocking<Unit> {

        val event = createRandomWithdrawalEvent().copy(value = EthUInt256.of(3))
        val balance = randomBalance(balance = EthUInt256.of(10))

        val reducedItem = reversedValueErc20BalanceReducer.reduce(balance, event)
        Assertions.assertThat(reducedItem.balance).isEqualTo(EthUInt256.of(13))
    }

    @Test
    fun `should calculate value on deposit event`() = runBlocking<Unit> {

        val event = createRandomDepositEvent().copy(value = EthUInt256.of(3))
        val balance = randomBalance(balance = EthUInt256.of(10))

        val reducedItem = reversedValueErc20BalanceReducer.reduce(balance, event)
        Assertions.assertThat(reducedItem.balance).isEqualTo(EthUInt256.of(7))
    }

    @Test
    fun `should calculate value on token approval event`() = runBlocking<Unit> {

        val event = createRandomTokenApprovalEvent().copy(value = EthUInt256.of(3))
        val balance = randomBalance(balance = EthUInt256.of(10))

        val reducedItem = reversedValueErc20BalanceReducer.reduce(balance, event)
        Assertions.assertThat(reducedItem.balance).isEqualTo(EthUInt256.of(10))
    }
}