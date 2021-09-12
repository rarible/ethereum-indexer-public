package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.CryptoPunksMarket
import com.rarible.protocol.dto.ActivityDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderPriceHistoryRecord
import com.rarible.protocol.order.core.service.PrepareTxService
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.*

@FlowPreview
abstract class AbstractCryptoPunkTest : AbstractIntegrationTest() {

    @Autowired
    protected lateinit var exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses

    @Autowired
    protected lateinit var prepareTxService: PrepareTxService

    protected lateinit var cryptoPunksMarket: CryptoPunksMarket

    private lateinit var lastKafkaInstant: Instant

    @BeforeEach
    fun clearKafkaQueue() {
        lastKafkaInstant = nowMillis()
    }

    @BeforeEach
    fun initializeCryptoPunksMarket() = runBlocking {
        cryptoPunksMarket = deployCryptoPunkMarket()

        // Override asset make balance service to correctly reflect ownership of CryptoPunks.
        // By default, this service returns 1 for all ownerships, even if a punk does not belong to this address.
        assetMakeBalanceAnswers = r@{ order ->
            val assetType = order.make.type as? CryptoPunksAssetType ?: return@r null
            if (assetType.marketAddress != cryptoPunksMarket.address()) {
                return@r null
            }
            val realOwner = cryptoPunksMarket.punkIndexToAddress(assetType.punkId.toBigInteger()).awaitSingle()
            if (order.maker == realOwner) EthUInt256.ONE else EthUInt256.ZERO
        }
    }

    protected fun checkActivityWasPublished(predicate: ActivityDto.() -> Boolean) =
        checkPublishedActivities { activities ->
            Assertions.assertTrue(activities.any(predicate)) {
                "Searched-for activity is not found in\n" + activities.joinToString("\n")
            }
        }

    protected fun checkPublishedActivities(assertBlock: suspend (List<ActivityDto>) -> Unit) = runBlocking {
        val activities = Collections.synchronizedList(arrayListOf<ActivityDto>())
        val job = async {
            consumer.receive().filter { it.value.date >= lastKafkaInstant }.collect { activities.add(it.value) }
        }
        try {
            Wait.waitAssert {
                assertBlock(activities)
            }
        } finally {
            job.cancelAndJoin()
        }
    }

    protected suspend fun deployCryptoPunkMarket(): CryptoPunksMarket {
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
