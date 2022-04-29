package com.rarible.protocol.erc20.core.repository

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.erc20.core.IntegrationTest
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.repository.data.randomErc20Deposit
import com.rarible.protocol.erc20.core.repository.data.randomErc20IncomeTransfer
import com.rarible.protocol.erc20.core.repository.data.randomErc20OutcomeTransfer
import com.rarible.protocol.erc20.core.repository.data.randomErc20Withdrawal
import com.rarible.protocol.erc20.core.repository.data.randomLogEvent
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address

@IntegrationTest
class TransferHistoryRepositoryIt {

    @Autowired
    lateinit var historyRepository: Erc20TransferHistoryRepository

    @Test
    fun `find balance history - with block filter`() = runBlocking {
        val token = randomAddress()
        val owner = randomAddress()

        val logEvent1 = randomLogEvent(randomErc20OutcomeTransfer(token, owner), blockNumber = 1)
        val logEvent2 = randomLogEvent(randomErc20IncomeTransfer(token, owner), blockNumber = 2)
        val logEvent3 = randomLogEvent(randomErc20Deposit(token, owner), blockNumber = 3)
        val logEvent4 = randomLogEvent(randomErc20Withdrawal(token, owner), blockNumber = 4)
        val logEvent5 = randomLogEvent(randomErc20Withdrawal(randomAddress(), owner), blockNumber = 4)

        saveAll(logEvent1, logEvent2, logEvent3, logEvent4, logEvent5)

        val logs = historyRepository.findBalanceLogEvents(BalanceId(token, owner), 1).collectList().awaitFirst()

        assertThat(logs).hasSize(3)
        assertEquals(logs[0].id, logEvent2.id)
        assertEquals(logs[1].id, logEvent3.id)
        assertEquals(logs[2].id, logEvent4.id)
    }

    @Test
    fun `find balance history - without block filter`() = runBlocking {
        val token = randomAddress()
        val owner = randomAddress()

        val logEvent1 = randomLogEvent(randomErc20OutcomeTransfer(token, owner), blockNumber = 1)
        val logEvent2 = randomLogEvent(randomErc20IncomeTransfer(token, owner), blockNumber = 2)
        val logEvent3 = randomLogEvent(randomErc20Withdrawal(randomAddress(), owner), blockNumber = 4)

        saveAll(logEvent1, logEvent2, logEvent3)

        val logs = historyRepository.findBalanceLogEvents(BalanceId(token, owner), null).collectList().awaitFirst()

        assertThat(logs).hasSize(2)
        assertEquals(logs[0].id, logEvent1.id)
        assertEquals(logs[1].id, logEvent2.id)
    }

    @Test
    fun `find balance events by owner - all types`() = runBlocking {
        val token = randomAddress()
        val owner = randomAddress()

        val incomeTransfer = randomErc20IncomeTransfer(token, owner)
        val outcomeTransfer = randomErc20OutcomeTransfer(token, owner)
        val deposit = randomErc20Deposit(token, owner)
        val withdrawal = randomErc20Withdrawal(token, owner)

        saveAll(token, incomeTransfer, outcomeTransfer, deposit, withdrawal)

        val logs = historyRepository.findOwnerLogEvents(token, owner).collectList().awaitFirst()
        assertThat(logs).hasSize(4)
        // All types of events should be returned
        assertEquals(logs[3].history, withdrawal)
        assertEquals(logs[2].history, deposit)
        assertEquals(logs[1].history, outcomeTransfer)
        assertEquals(logs[0].history, incomeTransfer)
    }

    @Test
    fun `find balance events by owner - filtered by token & owner`() = runBlocking {
        val token = randomAddress()
        val owner = randomAddress()

        val incomeTransfer = randomErc20IncomeTransfer(token, owner)
        val outcomeTransfer = randomErc20OutcomeTransfer(token, owner)

        val transfers = listOf(
            incomeTransfer,
            outcomeTransfer,
            randomErc20IncomeTransfer(token, randomAddress()),
            randomErc20OutcomeTransfer(randomAddress(), owner),
            randomErc20IncomeTransfer(randomAddress(), randomAddress()),
            randomErc20OutcomeTransfer(randomAddress(), randomAddress())
        )

        saveAll(token, *transfers.toTypedArray())

        val logs = historyRepository.findOwnerLogEvents(token, owner).collectList().awaitFirst()
        assertThat(logs).hasSize(2)
        assertEquals(logs[1].history, outcomeTransfer)
        assertEquals(logs[0].history, incomeTransfer)
    }

    @Test
    fun `find balance events by owner - filtered by from`() = runBlocking {
        val transfer11 = randomErc20OutcomeTransfer(token = Address.ONE(), owner = Address.ONE())
        val transfer12 = randomErc20OutcomeTransfer(token = Address.ONE(), owner = Address.TWO())
        val transfer21 = randomErc20OutcomeTransfer(token = Address.TWO(), owner = Address.ONE())

        saveAll(Address.ZERO(), transfer11, transfer12, transfer21)

        val logs1 = historyRepository.findOwnerLogEvents(
            from = BalanceId(Address.ONE(), Address.ONE())
        ).collectList().awaitFirst()

        assertThat(logs1).hasSize(2)
        assertEquals(logs1[0].history, transfer12)
        assertEquals(logs1[1].history, transfer21)

        val logs2 = historyRepository.findOwnerLogEvents(
            token = Address.ONE(),
            from = BalanceId(Address.ONE(), Address.ONE())
        ).collectList().awaitFirst()

        assertThat(logs2).hasSize(1)
        assertEquals(logs2[0].history, transfer12)
    }

    @Test
    fun `find balance events by token - without owner`() = runBlocking<Unit> {
        val token = randomAddress()

        val event1 = randomErc20IncomeTransfer(token, Address.ONE())
        val event2 = randomErc20OutcomeTransfer(randomAddress(), randomAddress())
        val event3 = randomErc20Deposit(token, Address.ZERO())

        saveAll(token, event1, event2, event3)

        val result = historyRepository.findBalanceLogEventsForToken(token, null)
            .collectList().awaitFirst()

        // Should be sorted by owner
        assertThat(result).hasSize(2)
        assertThat(result[0].data).isEqualTo(event3)
        assertThat(result[1].data).isEqualTo(event1)
    }

    @Test
    fun `find balance events by token - with owner`() = runBlocking<Unit> {
        val token = randomAddress()

        val event1 = randomErc20IncomeTransfer(token, Address.ONE())
        val event2 = randomErc20Deposit(token, Address.ZERO())

        saveAll(token, event1, event2)

        val result = historyRepository.findBalanceLogEventsForToken(token, Address.ZERO())
            .collectList().awaitFirst()

        assertThat(result).hasSize(1)
        assertThat(result[0].data).isEqualTo(event1)
    }

    private suspend fun saveAll(token: Address = randomAddress(), vararg histories: Erc20TokenHistory) {
        histories.forEachIndexed { index, history ->
            historyRepository.save(randomLogEvent(history).copy(address = token, index = index)).awaitFirst()
        }
    }

    private suspend fun saveAll(vararg logs: LogEvent) {
        logs.forEach { log ->
            historyRepository.save(log).awaitFirst()
        }
    }
}

