package com.rarible.protocol.order.core.repository.exchange

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.configuration.RepositoryConfiguration
import com.rarible.protocol.order.core.data.createOrderSideMatch
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ContextConfiguration

@MongoTest
@DataMongoTest
@MongoCleanup
@EnableAutoConfiguration
@ContextConfiguration(classes = [RepositoryConfiguration::class])
internal class ExchangeHistoryRepositoryTest {

    @Autowired
    private lateinit var exchangeHistoryRepository: ExchangeHistoryRepository

    @Test
    fun `get all by hash and target platform`() = runBlocking<Unit> {
        val history1 = createOrderSideMatch().copy(source = HistorySource.RARIBLE)
        val history2 = createOrderSideMatch().copy(source = HistorySource.RARIBLE)
        val history3 = createOrderSideMatch().copy(source = HistorySource.CRYPTO_PUNKS)
        val history4 = createOrderSideMatch().copy(source = HistorySource.OPEN_SEA)
        save(history1, history2, history3, history4)

        val targetOrderVersions = exchangeHistoryRepository
            .findLogEvents(null, null, platforms = listOf(HistorySource.RARIBLE))
            .collectList().awaitFirst()

        Assertions.assertThat(targetOrderVersions).hasSize(2)
        Assertions.assertThat(targetOrderVersions.map { (it.data as OrderExchangeHistory).hash }).containsExactlyInAnyOrder(history1.hash, history2.hash)
    }

    private suspend fun save(vararg history: OrderExchangeHistory) {
        history.forEach {
            exchangeHistoryRepository.save(
                LogEvent(
                    data = it,
                    address = randomAddress(),
                    topic = Word.apply(ByteArray(32)),
                    transactionHash = Word.apply(randomWord()),
                    status = LogEventStatus.CONFIRMED,
                    index = 0,
                    logIndex = 0,
                    minorLogIndex = 0
                )
            ).awaitFirst()
        }
    }
}
