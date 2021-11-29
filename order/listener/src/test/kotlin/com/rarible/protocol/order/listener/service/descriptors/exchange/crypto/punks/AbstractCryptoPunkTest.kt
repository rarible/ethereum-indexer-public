package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.protocol.contracts.exchange.crypto.punks.CryptoPunksMarket
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderPriceHistoryRecord
import com.rarible.protocol.order.core.service.PrepareTxService
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoField

@FlowPreview
abstract class AbstractCryptoPunkTest : AbstractIntegrationTest() {

    @Autowired
    protected lateinit var prepareTxService: PrepareTxService

    protected lateinit var cryptoPunksMarket: CryptoPunksMarket

    private lateinit var lastKafkaInstant: Instant

    @BeforeEach
    fun clearKafkaQueue() {
        // TODO: invent a better way of cleaning up the Kafka queue [RPN-1019].
        // Note! Here we should trim the time to seconds precious because the activities events will be created
        // with time taken from blockchain (it is in seconds).
        Thread.sleep(2001)
        lastKafkaInstant = Instant.now().with(ChronoField.NANO_OF_SECOND, 0)
    }

    @BeforeEach
    fun initializeCryptoPunksMarket() = runBlocking<Unit> {
        cryptoPunksMarket = deployCryptoPunkMarket()
    }

    private suspend fun deployCryptoPunkMarket(): CryptoPunksMarket {
        val (_, creatorSender) = newSender()
        val market = CryptoPunksMarket.deployAndWait(creatorSender, poller).awaitFirst()
        market.allInitialOwnersAssigned().execute().awaitFirst()
        exchangeContractAddresses.cryptoPunks = market.address()
        return market
    }

    protected fun createPriceHistory(time: Instant, make: Asset, take: Asset) =
        listOf(
            OrderPriceHistoryRecord(
                date = time,
                makeValue = make.value.value.toBigDecimal(if (make.type == EthAssetType) 18 else 0),
                takeValue = take.value.value.toBigDecimal(if (take.type == EthAssetType) 18 else 0)
            )
        )
}
