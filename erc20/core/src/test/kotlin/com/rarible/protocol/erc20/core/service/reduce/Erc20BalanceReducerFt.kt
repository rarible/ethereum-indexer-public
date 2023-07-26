package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.integration.AbstractIntegrationTest
import com.rarible.protocol.erc20.core.integration.IntegrationTest
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Event
import com.rarible.protocol.erc20.core.model.Erc20MarkedEvent
import com.rarible.protocol.erc20.core.randomDepositEvent
import com.rarible.protocol.erc20.core.randomIncomeTransferEvent
import com.rarible.protocol.erc20.core.randomOutcomeTransferEvent
import com.rarible.protocol.erc20.core.randomWithdrawalEvent
import com.rarible.protocol.erc20.core.repository.data.randomBalance
import com.rarible.protocol.erc20.core.withNewValues
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class Erc20BalanceReducerFt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var erc20BalaReducer: Erc20BalanceReducer

    @Test
    fun `should reduce simple incoming transfer event`() = runBlocking<Unit> {
        val token = randomAddress()
        val owner = randomAddress()

        val entity = randomBalance().copy(
            token = token,
            owner = owner,
            balance = EthUInt256.Companion.of(7)
        )
        val erc20IncomeTransferEvent = randomIncomeTransferEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1)
            .copy(
                value = EthUInt256.TEN,
                token = token,
                owner = owner
            )

        val reducedEntity = reduce(entity, erc20IncomeTransferEvent)
        Assertions.assertThat(reducedEntity.balance).isEqualTo(EthUInt256.of(17))
    }

    @Test
    fun `should reduce incoming transfer event and revert event with exist entity`() = runBlocking<Unit> {
        val token = randomAddress()
        val owner = randomAddress()

        val entity = randomBalance().copy(
            token = token,
            owner = owner,
            balance = EthUInt256.Companion.of(7)
        )
        val event = randomIncomeTransferEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(
                value = EthUInt256.TEN,
                token = token,
                owner = owner
            )

        val revertedEvent = event
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)

        val reducedItem = reduce(entity, event, revertedEvent)
        Assertions.assertThat(reducedItem.balance).isEqualTo(EthUInt256.of(7))
    }

    @Test
    fun `should reduce chain events with reverted events with exist entity`() = runBlocking<Unit> {
        val token = randomAddress()
        val owner = randomAddress()

        val entity = randomBalance().copy(
            token = token,
            owner = owner,
            balance = EthUInt256.Companion.of(7)
        )
        val incomeTransferEvent = randomIncomeTransferEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(
                value = EthUInt256.TEN,
                token = token,
                owner = owner
            )

        val outcomeTransferEvent = randomOutcomeTransferEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 2, logIndex = 2, minorLogIndex = 2)
            .copy(
                value = EthUInt256.ONE,
                token = token,
                owner = owner
            )

        val incomeTransferSecondEvent = randomIncomeTransferEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 3, logIndex = 3, minorLogIndex = 3)
            .copy(
                value = EthUInt256.TEN,
                token = token,
                owner = owner
            )

        val revertedEvent = incomeTransferSecondEvent
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 3, logIndex = 3, minorLogIndex = 3)

        val withdrawalEvent = randomWithdrawalEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 4, logIndex = 4, minorLogIndex = 4)
            .copy(
                value = EthUInt256.of(6),
                token = token,
                owner = owner
            )

        val depositEvent = randomDepositEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 5, logIndex = 5, minorLogIndex = 5)
            .copy(
                value = EthUInt256.of(4),
                token = token,
                owner = owner
            )

        val reducedItem = reduce(
            entity,
            incomeTransferEvent,
            outcomeTransferEvent,
            incomeTransferSecondEvent,
            revertedEvent,
            withdrawalEvent,
            depositEvent
        )
        Assertions.assertThat(reducedItem.balance).isEqualTo(EthUInt256.of(14))
    }

    private suspend fun reduce(item: Erc20Balance, events: List<Erc20Event>): Erc20Balance {
        return reduce(item, *events.toTypedArray())
    }

    private suspend fun reduce(item: Erc20Balance, vararg events: Erc20Event): Erc20Balance {
        return events.fold(item) { entity, event ->
            erc20BalaReducer.reduce(entity, Erc20MarkedEvent(event))
        }
    }
}
