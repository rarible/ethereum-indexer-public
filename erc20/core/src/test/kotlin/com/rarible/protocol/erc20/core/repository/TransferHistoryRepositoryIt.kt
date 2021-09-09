package com.rarible.protocol.erc20.core.repository

import com.rarible.core.test.ext.MongoTest
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.erc20.core.IntegrationTest
import com.rarible.protocol.erc20.core.configuration.CoreConfiguration
import com.rarible.protocol.erc20.core.model.*
import com.rarible.protocol.erc20.core.repository.data.createAddress
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.ContextConfiguration
import scalether.domain.Address
import java.util.*

@MongoTest
@DataMongoTest
@ContextConfiguration(classes = [CoreConfiguration::class])
class TransferHistoryRepositoryIt {

    @Autowired
    lateinit var historyRepository: Erc20TransferHistoryRepository

    @Autowired
    lateinit var mongo: ReactiveMongoTemplate

    @Test
    fun `should save and get all types history`() = runBlocking {
        val token = createAddress()
        val owner = createAddress()

        val incomeTransfer = Erc20OutcomeTransfer(owner = owner, token = token, date = Date(), value = EthUInt256.of(1))
        val outcomeTransfer = Erc20IncomeTransfer(owner = owner, token = token, date = Date(), value = EthUInt256.of(1))
        val deposit = Erc20Deposit(owner = owner, token = token, date = Date(), value = EthUInt256.of(1))
        val withdrawal = Erc20Withdrawal(owner = owner, token = token, date = Date(), value = EthUInt256.of(1))

        prepareStorage(token, incomeTransfer, outcomeTransfer, deposit, withdrawal)

        val logs = historyRepository.findOwnerLogEvents(token, owner).collectList().awaitFirst()
        assertThat(logs).hasSize(4)
        assertEquals(logs[3].history, withdrawal)
        assertEquals(logs[2].history, deposit)
        assertEquals(logs[1].history, outcomeTransfer)
        assertEquals(logs[0].history, incomeTransfer)
    }

    @Test
    fun `should save and get all types history from target block`() = runBlocking {
        val token = createAddress()
        val owner = createAddress()

        val logEvent1 = createLogEvent(
            Erc20OutcomeTransfer(owner = owner, token = token, date = Date(), value = EthUInt256.of(1))
        ).copy(blockNumber = 1)

        val logEvent2 = createLogEvent(
            Erc20IncomeTransfer(owner = owner, token = token, date = Date(), value = EthUInt256.of(1))
        ).copy(blockNumber = 2)

        val logEvent3 = createLogEvent(
            Erc20Deposit(owner = owner, token = token, date = Date(), value = EthUInt256.of(1))
        ).copy(blockNumber = 3)

        val logEvent4 = createLogEvent(
            Erc20Withdrawal(owner = owner, token = token, date = Date(), value = EthUInt256.of(1))
        ).copy(blockNumber = 4)

        val logEvent5 = createLogEvent(
            Erc20Withdrawal(owner = owner, token = createAddress(), date = Date(), value = EthUInt256.of(1))
        ).copy(blockNumber = 4)

        prepareStorage(logEvent1, logEvent2, logEvent3, logEvent4, logEvent5)

        val logs = historyRepository.findBalanceLogEvents(BalanceId(token, owner), 1).collectList().awaitFirst()

        assertThat(logs).hasSize(3)
        assertEquals(logs[0].id, logEvent2.id)
        assertEquals(logs[1].id, logEvent3.id)
        assertEquals(logs[2].id, logEvent4.id)
    }

    @Test
    fun `should get transfers history by token and owner`() = runBlocking {
        val token = createAddress()
        val owner = createAddress()

        val incomeTransfer = Erc20OutcomeTransfer(owner = owner, token = token, date = Date(), value = EthUInt256.of(1))
        val outcomeTransfer = Erc20IncomeTransfer(owner = owner, token = token, date = Date(), value = EthUInt256.of(1))

        val transfers = listOf(
            incomeTransfer,
            outcomeTransfer,
            Erc20IncomeTransfer(owner = createAddress(), token = token, date = Date(), value = EthUInt256.of(1)),
            Erc20OutcomeTransfer(owner = owner, token = createAddress(), date = Date(), value = EthUInt256.of(1)),
            Erc20IncomeTransfer(owner = createAddress(), token = createAddress(), date = Date(), value = EthUInt256.of(1)),
            Erc20OutcomeTransfer(owner = createAddress(), token = createAddress(), date = Date(), value = EthUInt256.of(1))
        )

        prepareStorage(token, *transfers.toTypedArray())

        val logs = historyRepository.findOwnerLogEvents(token, owner).collectList().awaitFirst()
        assertThat(logs).hasSize(2)
        assertEquals(logs[1].history, outcomeTransfer)
        assertEquals(logs[0].history, incomeTransfer)
    }

    @Test
    fun `should get transfers history using from`() = runBlocking {

        val transfer11 = Erc20OutcomeTransfer(owner = Address.ONE(), token = Address.ONE(), date = Date(), value = EthUInt256.of(1))
        val transfer12 = Erc20OutcomeTransfer(owner = Address.TWO(), token = Address.ONE(), date = Date(), value = EthUInt256.of(1))
        val transfer21 = Erc20OutcomeTransfer(owner = Address.ONE(), token = Address.TWO(), date = Date(), value = EthUInt256.of(1))

        prepareStorage(
            Address.ZERO(),
            transfer11,
            transfer12,
            transfer21
        )

        val logs1 = historyRepository.findOwnerLogEvents(from = Wallet(Address.ONE(), Address.ONE())).collectList().awaitFirst()
        assertThat(logs1).hasSize(2)
        assertEquals(logs1[0].history, transfer12)
        assertEquals(logs1[1].history, transfer21)

        val logs2 = historyRepository.findOwnerLogEvents(token = Address.ONE(), from = Wallet(Address.ONE(), Address.ONE())).collectList().awaitFirst()
        assertThat(logs2).hasSize(1)
        assertEquals(logs2[0].history, transfer12)
    }

    private suspend fun prepareStorage(token: Address = createAddress(), vararg histories: Erc20TokenHistory) {
        histories.forEachIndexed { index, history ->
            historyRepository.save(createLogEvent(history).copy(address = token, index = index)).awaitFirst()
        }
    }

    private suspend fun prepareStorage(vararg logs: LogEvent) {
        logs.forEach { log ->
            historyRepository.save(log).awaitFirst()
        }
    }

    private fun createLogEvent(history: Erc20TokenHistory): LogEvent {
        return LogEvent(
            data = history,
            address = history.token,
            topic = word(),
            transactionHash = word(),
            status = LogEventStatus.CONFIRMED,
            blockNumber = 1,
            logIndex = 0,
            minorLogIndex = 0,
            index = 0
        )
    }

    private fun word(): Word = Word(RandomUtils.nextBytes(32))

    @BeforeEach
    fun cleanDatabase() {
        mongo.collectionNames
            .filter { !it.startsWith("system") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()
    }
}
