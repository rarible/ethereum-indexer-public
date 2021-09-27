package com.rarible.protocol.order.listener.job

import com.rarible.core.common.nowMillis
import com.rarible.core.test.containers.MongodbReactiveBaseTest
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.event.NftOrdersPriceUpdateListener
import com.rarible.protocol.order.core.event.OrderListener
import com.rarible.protocol.order.core.event.OrderVersionListener
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.provider.ProtocolCommissionProvider
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.balance.AssetMakeBalanceProvider
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.service.order.OrderPriceUpdateService
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.query.Query
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigDecimal

internal class OrderPricesUpdateJobTest : MongodbReactiveBaseTest() {

    @BeforeEach
    fun cleanDatabase() {
        val mongo = createReactiveMongoTemplate()

        mongo.collectionNames
            .filter { !it.startsWith("system") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()
    }

    private val orderRepository = MongoOrderRepository(createReactiveMongoTemplate())
    private val orderVersionRepository = OrderVersionRepository(createReactiveMongoTemplate())
    private val priceUpdateService = mockk<PriceUpdateService>()
    private val exchangeHistoryRepository = ExchangeHistoryRepository(createReactiveMongoTemplate())
    private val assetMakeBalanceProvider = mockk<AssetMakeBalanceProvider>()
    private val priceNormalizer = mockk<PriceNormalizer>()
    private val protocolCommissionProvider = mockk<ProtocolCommissionProvider>()
    private val orderVersionListener = mockk<OrderVersionListener>()
    private val orderListener = mockk<OrderListener>()
    private val nftOrdersPriceUpdateListener = mockk<NftOrdersPriceUpdateListener>()
    private val orderReduceService = OrderReduceService(
        exchangeHistoryRepository = exchangeHistoryRepository,
        orderRepository = orderRepository,
        orderVersionRepository = orderVersionRepository,
        assetMakeBalanceProvider = assetMakeBalanceProvider,
        protocolCommissionProvider = protocolCommissionProvider,
        priceNormalizer = priceNormalizer,
        priceUpdateService = priceUpdateService
    )
    private val orderUpdateService = OrderUpdateService(
        orderVersionRepository = orderVersionRepository,
        orderRepository = orderRepository,
        assetMakeBalanceProvider = assetMakeBalanceProvider,
        protocolCommissionProvider = protocolCommissionProvider,
        priceUpdateService = priceUpdateService,
        orderReduceService = orderReduceService,
        orderVersionListener = orderVersionListener,
        orderListener = orderListener
    )
    private val orderPriceUpdateService = OrderPriceUpdateService(
        orderRepository = orderRepository,
        orderVersionRepository = orderVersionRepository,
        priceUpdateService = priceUpdateService
    )
    private val orderPricesUpdateJob = OrderPricesUpdateJob(
        properties = OrderListenerProperties(priceUpdateEnabled = true),
        orderPriceUpdateService = orderPriceUpdateService,
        reactiveMongoTemplate = createReactiveMongoTemplate(),
        nftOrdersPriceUpdateListener = nftOrdersPriceUpdateListener
    )

    @Test
    fun `should update only the active order and its version`() = runBlocking {
        coEvery { priceNormalizer.normalize(any()) } returns BigDecimal.ZERO to BigDecimal.ZERO
        coEvery { orderVersionListener.onOrderVersion(any()) } returns Unit
        coEvery { orderListener.onOrder(any()) } returns Unit
        coEvery { nftOrdersPriceUpdateListener.onNftOrders(any(), any(), any()) } returns Unit
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns EthUInt256.TEN
        coEvery { protocolCommissionProvider.get() } returns EthUInt256.ZERO

        val newMakeUsd = BigDecimal.valueOf(2)
        val newTakePriceUsd = BigDecimal.valueOf(3)
        val order1Make = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        val order2Make = Asset(EthAssetType, EthUInt256.TEN)

        coEvery { priceUpdateService.getAssetsUsdValue(any(), any(), any()) } answers {
            when (arg<Asset>(0)) {
                order1Make -> OrderUsdValue.BidOrder(makeUsd = newMakeUsd, takePriceUsd = newTakePriceUsd)
                else -> error("Order 2 must not be updated!")
            }
        }

        val order1 = orderUpdateService.save(createOrderVersion().copy(make = order1Make))
        val order2 = orderUpdateService.save(createOrderVersion().copy(make = order2Make))
        cancelOrder(order2.hash)

        orderPricesUpdateJob.updateOrdersPrices()

        val updatedOrder1 = orderRepository.findById(order1.hash)
        assertThat(updatedOrder1?.makeUsd).isEqualTo(newMakeUsd)
        assertThat(updatedOrder1?.takePriceUsd).isEqualTo(newTakePriceUsd)
        assertThat(updatedOrder1?.takeUsd).isNull()
        assertThat(updatedOrder1?.makePriceUsd).isNull()

        val updatedOrderVersion1 = orderVersionRepository.findAllByHash(order1.hash).toList().single()
        assertThat(updatedOrderVersion1.makeUsd).isEqualTo(newMakeUsd)
        assertThat(updatedOrderVersion1.takePriceUsd).isEqualTo(newTakePriceUsd)
        assertThat(updatedOrderVersion1.takeUsd).isNull()
        assertThat(updatedOrderVersion1.makePriceUsd).isNull()
    }

    private suspend fun cancelOrder(orderHash: Word) {
        exchangeHistoryRepository.save(
            LogEvent(
                data = OrderCancel(
                    hash = orderHash,
                    date = nowMillis(),

                    // Do not matter.
                    maker = null,
                    make = null,
                    take = null,
                    source = HistorySource.RARIBLE
                ),
                address = Address.ZERO(),
                topic = Word.apply(randomWord()),
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                index = 0,
                logIndex = 0,
                minorLogIndex = 0
            )
        ).awaitFirst()
        orderReduceService.updateOrder(orderHash)
    }
}
