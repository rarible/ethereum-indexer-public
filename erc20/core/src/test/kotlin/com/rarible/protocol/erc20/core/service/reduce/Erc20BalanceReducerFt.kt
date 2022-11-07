package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.createRandomDepositEvent
import com.rarible.protocol.erc20.core.createRandomIncomeTransferEvent
import com.rarible.protocol.erc20.core.createRandomOutcomeTransferEvent
import com.rarible.protocol.erc20.core.createRandomWithdrawalEvent
import com.rarible.protocol.erc20.core.integration.AbstractIntegrationTest
import com.rarible.protocol.erc20.core.integration.IntegrationTest
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Event
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
        val erc20IncomeTransferEvent = createRandomIncomeTransferEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 1)
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
        val event = createRandomIncomeTransferEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(
                value = EthUInt256.TEN,
                token = token,
                owner = owner
            )

        val revertedEvent = event
            .withNewValues(EthereumLogStatus.REVERTED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)

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
        val incomeTransferEvent = createRandomIncomeTransferEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(
                value = EthUInt256.TEN,
                token = token,
                owner = owner
            )

        val outcomeTransferEvent = createRandomOutcomeTransferEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 2, logIndex = 2, minorLogIndex = 2)
            .copy(
                value = EthUInt256.ONE,
                token = token,
                owner = owner
            )

        val incomeTransferSecondEvent = createRandomIncomeTransferEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 3, logIndex = 3, minorLogIndex = 3)
            .copy(
                value = EthUInt256.TEN,
                token = token,
                owner = owner
            )

        val revertedEvent = incomeTransferSecondEvent
            .withNewValues(EthereumLogStatus.REVERTED, blockNumber = 3, logIndex = 3, minorLogIndex = 3)

        val withdrawalEvent = createRandomWithdrawalEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 4, logIndex = 4, minorLogIndex = 4)
            .copy(
                value = EthUInt256.of(6),
                token = token,
                owner = owner
            )

        val depositEvent = createRandomDepositEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 5, logIndex = 5, minorLogIndex = 5)
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
            erc20BalaReducer.reduce(entity, event)
        }
    }
}