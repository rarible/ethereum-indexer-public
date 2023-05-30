package com.rarible.protocol.erc20.listener.service.balance

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.ext.KafkaTestExtension
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.Erc20BalanceEventTopicProvider
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.erc20.core.configuration.Erc20IndexerProperties
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20IncomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20OutcomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.repository.Erc20BalanceRepository
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.listener.data.randomErc20ReduceEvent
import com.rarible.protocol.erc20.listener.test.AbstractIntegrationTest
import com.rarible.protocol.erc20.listener.test.IntegrationTest
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.math.BigInteger
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

@Deprecated("It uses v1 reducer")
@FlowPreview
@IntegrationTest
internal class BalanceReduceServiceV1It : AbstractIntegrationTest() {

    @Autowired
    private lateinit var properties: Erc20IndexerProperties

    @Autowired
    private lateinit var application: ApplicationEnvironmentInfo

    @Autowired
    protected lateinit var balanceReduceService: Erc20BalanceReduceServiceV1

    @Autowired
    protected lateinit var historyRepository: Erc20TransferHistoryRepository

    @Autowired
    protected lateinit var erc20BalanceRepository: Erc20BalanceRepository

    @Test
    fun `should calculate balance for not existed balance`() = runBlocking<Unit> {
        val consumer = createConsumer()
        val events = CopyOnWriteArrayList<KafkaMessage<Erc20BalanceEventDto>>()
        val job = async {
            consumer
                .receiveAutoAck()
                .collect { events.add(it) }
        }

        val walletToken = randomAddress()
        val walletOwner = randomAddress()
        val balanceId = BalanceId(walletToken, walletOwner)

        val transfer = mockk<Erc20TokenHistory> {
            every { token } returns walletToken
            every { owner } returns walletOwner
        }

        val createdDate = Date(nowMillis().minusSeconds(30).toEpochMilli())
        val otherActionsDate = Date(nowMillis().minusSeconds(20).toEpochMilli())
        val finalUpdateDate = Date(nowMillis().minusSeconds(10).toEpochMilli())

        prepareStorage(
            walletToken,
            Erc20IncomeTransfer(owner = walletOwner, token = walletToken, date = createdDate, value = EthUInt256.of(3)),
            Erc20IncomeTransfer(
                owner = walletOwner, token = walletToken, date = otherActionsDate, value = EthUInt256.of(7)
            ),
            Erc20OutcomeTransfer(
                owner = walletOwner, token = walletToken, date = otherActionsDate, value = EthUInt256.of(4)
            ),
            Erc20OutcomeTransfer(
                owner = walletOwner, token = walletToken, date = finalUpdateDate, value = EthUInt256.of(5)
            )
        )

        balanceReduceService.onEvents(listOf(randomErc20ReduceEvent(transfer)))

        val balance = erc20BalanceRepository.get(balanceId)!!
        assertThat(balance.balance).isEqualTo(EthUInt256.ONE)
        assertThat(balance.lastUpdatedAt!!.toEpochMilli()).isEqualTo(finalUpdateDate.time)
        assertThat(balance.createdAt!!.toEpochMilli()).isEqualTo(createdDate.time)
        assertThat(balance.blockNumber).isGreaterThan(0L)

        Wait.waitAssert {
            assertThat(events).hasSizeGreaterThanOrEqualTo(1)
            val event = events.firstOrNull { it.value.balanceId == balance.id.stringValue }!!
            assertThat(event.value.createdAt).isEqualTo(balance.createdAt)
            assertThat(event.value.lastUpdatedAt).isEqualTo(balance.lastUpdatedAt)
        }
        job.cancel()
    }

    @Test
    fun `should calculate balance for existed balance`() = runBlocking<Unit> {
        val consumer = createConsumer()
        val events = CopyOnWriteArrayList<KafkaMessage<Erc20BalanceEventDto>>()
        val job = async {
            consumer
                .receiveAutoAck()
                .collect { events.add(it) }
        }

        val walletToken = randomAddress()
        val walletOwner = randomAddress()
        val balanceId = BalanceId(walletToken, walletOwner)

        val currentBalance = Erc20Balance(
            token = walletToken,
            owner = walletOwner,
            balance = EthUInt256.of(1),
            createdAt = nowMillis().minusSeconds(300),
            lastUpdatedAt = nowMillis().minusSeconds(600),
            blockNumber = null
        )
        erc20BalanceRepository.save(currentBalance)

        val transfer = mockk<Erc20TokenHistory> {
            every { token } returns walletToken
            every { owner } returns walletOwner
        }

        val incomeDate = Date(nowMillis().minusSeconds(30).toEpochMilli())
        val outcomeDate = Date(nowMillis().minusSeconds(10).toEpochMilli())

        prepareStorage(
            walletToken,
            Erc20IncomeTransfer(owner = walletOwner, token = walletToken, date = incomeDate, value = EthUInt256.of(10)),
            Erc20OutcomeTransfer(owner = walletOwner, token = walletToken, date = outcomeDate, value = EthUInt256.of(5))
        )
        balanceReduceService.onEvents(listOf(randomErc20ReduceEvent(transfer)))

        val balance = erc20BalanceRepository.get(balanceId)!!
        assertThat(balance.balance).isEqualTo(EthUInt256.of(5))
        assertThat(balance.createdAt).isEqualTo(currentBalance.createdAt)
        assertThat(balance.lastUpdatedAt!!.toEpochMilli()).isEqualTo(outcomeDate.time)

        Wait.waitAssert {
            assertThat(events)
                .hasSizeGreaterThanOrEqualTo(1)
                .satisfies(Consumer { events ->
                    val event = events.firstOrNull { it.value.balanceId == balance.id.stringValue }!!
                    assertThat(event.value.createdAt).isEqualTo(balance.createdAt)
                    assertThat(event.value.lastUpdatedAt).isEqualTo(balance.lastUpdatedAt)
                })
        }
        job.cancel()
    }

    @Test
    fun `shouldn't send duplication event for existed balance`() = runBlocking<Unit> {
        val consumer = createConsumer()
        val events = CopyOnWriteArrayList<KafkaMessage<Erc20BalanceEventDto>>()
        val job = async {
            consumer
                .receiveAutoAck()
                .collect { events.add(it) }
        }

        val walletToken = randomAddress()
        val walletOwner = randomAddress()
        val balanceId = BalanceId(walletToken, walletOwner)
        val incomeDate = Date(nowMillis().minusSeconds(30).toEpochMilli())
        val outcomeDate = Date(nowMillis().minusSeconds(10).toEpochMilli())

        val currentBalance = Erc20Balance(
            token = walletToken,
            owner = walletOwner,
            balance = EthUInt256.of(5),
            createdAt = incomeDate.toInstant(),
            lastUpdatedAt = incomeDate.toInstant(),
            blockNumber = 1
        )
        erc20BalanceRepository.save(currentBalance)

        val transfer = mockk<Erc20TokenHistory> {
            every { token } returns walletToken
            every { owner } returns walletOwner
        }

        prepareStorage(
            walletToken,
            Erc20IncomeTransfer(owner = walletOwner, token = walletToken, date = incomeDate, value = EthUInt256.of(5))
        )
        balanceReduceService.onEvents(listOf(randomErc20ReduceEvent(transfer)))

        val balance = erc20BalanceRepository.get(balanceId)!!
        assertThat(balance.balance).isEqualTo(EthUInt256.of(5))
        assertThat(balance.createdAt).isEqualTo(currentBalance.createdAt)
        assertThat(balance.lastUpdatedAt!!.toEpochMilli()).isEqualTo(incomeDate.time)

        // no changes
        Wait.waitAssert {
            assertThat(events).hasSize(0)
        }

        // let's trigger the balance changing
        prepareStorage(
            walletToken,
            Erc20OutcomeTransfer(owner = walletOwner, token = walletToken, date = outcomeDate, value = EthUInt256.of(5))
        )
        balanceReduceService.onEvents(listOf(randomErc20ReduceEvent(transfer)))
        Wait.waitAssert {
            assertThat(events)
                .hasSize(1)
                .satisfies(Consumer { events ->
                    val event = events.first { it.value.balanceId == balance.id.stringValue }.value as Erc20BalanceUpdateEventDto
                    assertThat(event.balance.balance).isEqualTo(BigInteger.ZERO)
                    assertThat(event.balance.createdAt).isEqualTo(incomeDate.toInstant())
                    assertThat(event.balance.lastUpdatedAt).isEqualTo(outcomeDate.toInstant())
                })
        }

        job.cancel()
    }

    private suspend fun prepareStorage(token: Address, vararg histories: Erc20TokenHistory) {
        histories.forEachIndexed { index, history ->
            historyRepository.save(
                LogEvent(
                    data = history, address = token, topic = word(), transactionHash = word(),
                    status = LogEventStatus.CONFIRMED, blockNumber = 1,
                    logIndex = 0, minorLogIndex = index, index = 0
                )
            ).awaitFirst()
        }
    }

    private fun createConsumer(): RaribleKafkaConsumer<Erc20BalanceEventDto> {
        return RaribleKafkaConsumer(
            clientId = "test-consumer",
            consumerGroup = "test-group",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = Erc20BalanceEventDto::class.java,
            defaultTopic = Erc20BalanceEventTopicProvider.getTopic(application.name, properties.blockchain.value),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    private fun word(): Word = Word(RandomUtils.nextBytes(32))
}
