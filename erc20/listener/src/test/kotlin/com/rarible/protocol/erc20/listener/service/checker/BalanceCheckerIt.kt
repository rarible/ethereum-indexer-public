package com.rarible.protocol.erc20.listener.service.checker

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.wait.BlockingWait.waitAssert
import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.erc20.core.metric.CheckerMetrics
import com.rarible.protocol.erc20.core.model.Erc20IncomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.listener.data.randomErc20ReduceEvent
import com.rarible.protocol.erc20.listener.service.balance.Erc20BalanceReduceServiceV1
import com.rarible.protocol.erc20.listener.test.AbstractIntegrationTest
import com.rarible.protocol.erc20.listener.test.IntegrationTest
import io.daonomic.rpc.domain.Word
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import scalether.domain.Address
import java.util.*


@FlowPreview
@IntegrationTest
class BalanceCheckerIt : AbstractIntegrationTest() {

    @Autowired
    protected lateinit var balanceReduceService: Erc20BalanceReduceServiceV1

    @Autowired
    protected lateinit var historyRepository: Erc20TransferHistoryRepository

    @Autowired
    lateinit var mongoTemplate: ReactiveMongoTemplate

    @Autowired
    lateinit var meterRegistry: MeterRegistry

    @Autowired
    lateinit var blockchain: Blockchain

    @BeforeEach
    @AfterEach
    fun cleanUp() = runBlocking<Unit> {
        mongoTemplate.remove(Query(), "erc20_history").awaitSingle()
        mongoTemplate.remove(Query(), "erc20_balance").awaitSingle()
    }

    @Test
    fun `should check balance`() = runBlocking<Unit> {
        val walletToken = randomAddress()
        val walletOwner = randomAddress()

        val transfer = mockk<Erc20TokenHistory> {
            every { token } returns walletToken
            every { owner } returns walletOwner
        }

        val createdDate = Date(nowMillis().minusSeconds(30).toEpochMilli())

        prepareStorage(
            walletToken,
            Erc20IncomeTransfer(owner = walletOwner, token = walletToken, date = createdDate, value = EthUInt256.of(1))
        )

        println("!!! owner=${walletOwner}, token=${walletToken}")

        balanceReduceService.onEvents(listOf(randomErc20ReduceEvent(transfer)))

        waitAssert {
            assertThat(counter(CheckerMetrics.BALANCE_INCOMING)).isEqualTo(1.0)
            assertThat(counter(CheckerMetrics.BALANCE_CHECK)).isEqualTo(1.0)
            assertThat(counter(CheckerMetrics.BALANCE_INVALID)).isEqualTo(0.0)
        }
    }

    private fun counter(name: String): Double {
        return meterRegistry.counter(name, listOf(ImmutableTag("blockchain", blockchain.name.lowercase()))).count()
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

    private fun word(): Word = Word(RandomUtils.nextBytes(32))
}
