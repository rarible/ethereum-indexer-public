package com.rarible.protocol.order.core.repository.exchange

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.data.createOrderSideMatch
import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@IntegrationTest
internal class ExchangeHistoryRepositoryTest : AbstractIntegrationTest() {

    @BeforeEach
    fun setup() = runBlocking {
        exchangeHistoryRepository.createIndexes()
    }

    @Test
    fun `get all by hash and target platform`() = runBlocking<Unit> {
        val history1 = createOrderSideMatch().copy(source = HistorySource.RARIBLE)
        val history2 = createOrderSideMatch().copy(source = HistorySource.RARIBLE)
        val history3 = createOrderSideMatch().copy(source = HistorySource.CRYPTO_PUNKS)
        val history4 = createOrderSideMatch().copy(source = HistorySource.OPEN_SEA)
        save(history1, history2, history3, history4)

        val targetOrderVersions = exchangeHistoryRepository
            .findReversedEthereumLogRecords(null, null, platforms = listOf(HistorySource.RARIBLE))
            .collectList().awaitFirst()

        Assertions.assertThat(targetOrderVersions).hasSize(2)
        Assertions.assertThat(targetOrderVersions.map { (it.data as OrderExchangeHistory).hash }).containsExactlyInAnyOrder(history1.hash, history2.hash)
    }

    @Test
    fun `get all - ignored events`() = runBlocking<Unit> {
        val history1 = createOrderSideMatch().copy(source = HistorySource.OPEN_SEA, ignoredEvent = true)
        val history2 = createOrderSideMatch().copy(source = HistorySource.OPEN_SEA, ignoredEvent = true)
        val history3 = createOrderSideMatch().copy(source = HistorySource.OPEN_SEA)
        val history4 = createOrderSideMatch().copy(source = HistorySource.RARIBLE, ignoredEvent = true)
        save(history1, history2, history3, history4)

        val targetHistory = exchangeHistoryRepository
            .findIgnoredEvents(null, HistorySource.OPEN_SEA)
            .toList()

        Assertions.assertThat(targetHistory).hasSize(2)
        Assertions.assertThat(targetHistory.map { (it.data as OrderExchangeHistory).hash }).containsExactlyInAnyOrder(history1.hash, history2.hash)
    }

    private suspend fun save(vararg history: OrderExchangeHistory) {
        history.forEach {
            exchangeHistoryRepository.save(
                ReversedEthereumLogRecord(
                    id = ObjectId().toHexString(),
                    data = it,
                    address = randomAddress(),
                    topic = Word.apply(ByteArray(32)),
                    transactionHash = randomWord(),
                    status = EthereumBlockStatus.CONFIRMED,
                    index = 0,
                    logIndex = 0,
                    minorLogIndex = 0
                )
            ).awaitFirst()
        }
    }
}
