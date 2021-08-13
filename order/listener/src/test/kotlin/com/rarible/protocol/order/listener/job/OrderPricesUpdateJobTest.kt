package com.rarible.protocol.order.listener.job

import com.rarible.core.test.containers.MongodbReactiveBaseTest
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderUsdValue
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.data.createOrderVersion
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.query.Query
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

    private val orderPricesUpdateJob = OrderPricesUpdateJob(
        properties = OrderListenerProperties(priceUpdateEnabled = true),
        priceUpdateService = priceUpdateService,
        orderVersionRepository = orderVersionRepository,
        reactiveMongoTemplate = createReactiveMongoTemplate()
    )

    @Test
    fun `should update order and all versions`() = runBlocking<Unit> {
        val newMakePriceUsd1 = BigDecimal.valueOf(1)
        val newMakeUsd1 = BigDecimal.valueOf(2)
        val newTakePriceUsd1 = BigDecimal.valueOf(3)
        val newTakeUsd1 = BigDecimal.valueOf(6)

        val order1 = createOrder().copy(cancelled = false, makeStock = EthUInt256.TEN)
        val orderVersion1 = createOrderVersion().copy(hash = order1.hash, make = order1.make, take = order1.take)
        val orderVersion2 = createOrderVersion().copy(hash = order1.hash, make = order1.make, take = order1.take)

        val order2 = createOrder().copy(cancelled = false, makeStock = EthUInt256.TEN)
        val orderVersion3 = createOrderVersion().copy(hash = order2.hash, make = order2.make, take = order2.take)
        val orderVersion4 = createOrderVersion().copy(hash = order2.hash, make = order2.make, take = order2.take)

        val order3 = createOrder().copy(cancelled = true)
        val order4 = createOrder().copy(makeStock = EthUInt256.ZERO)

        save(order1, order2, order3, order4)
        save(orderVersion1, orderVersion2, orderVersion3, orderVersion4)

        coEvery { priceUpdateService.getAssetsUsdValue(any(), any(), any()) } answers {
            when (arg<Asset>(0)) {
                order1.make -> OrderUsdValue.BidOrder(makeUsd = newMakeUsd1, takePriceUsd = newTakePriceUsd1)
                order2.make -> OrderUsdValue.SellOrder(takeUsd = newTakeUsd1, makePriceUsd = newMakePriceUsd1)
                else -> throw IllegalArgumentException("Unexpected order make type")
            }
        }
        orderPricesUpdateJob.updateOrdersPrices()

        val updatedOrder1 = orderRepository.findById(order1.hash)
        assertThat(updatedOrder1?.makeUsd).isEqualTo(newMakeUsd1)
        assertThat(updatedOrder1?.takePriceUsd).isEqualTo(newTakePriceUsd1)
        assertThat(updatedOrder1?.takeUsd).isNull()
        assertThat(updatedOrder1?.makePriceUsd).isNull()

        val updatedOrder2 = orderRepository.findById(order2.hash)
        assertThat(updatedOrder2?.makeUsd).isNull()
        assertThat(updatedOrder2?.takePriceUsd).isNull()
        assertThat(updatedOrder2?.takeUsd).isEqualTo(newTakeUsd1)
        assertThat(updatedOrder2?.makePriceUsd).isEqualTo(newMakePriceUsd1)

        val updatedOrderVersions1 = orderVersionRepository.findAllByHash(order1.hash).toList()
        assertThat(updatedOrderVersions1).hasSize(2)
        updatedOrderVersions1.forEach {
            assertThat(it.makeUsd).isEqualTo(newMakeUsd1)
            assertThat(it.takePriceUsd).isEqualTo(newTakePriceUsd1)
            assertThat(it.takeUsd).isNull()
            assertThat(it.makePriceUsd).isNull()
        }

        val updatedOrderVersions2 = orderVersionRepository.findAllByHash(order2.hash).toList()
        assertThat(updatedOrderVersions2).hasSize(2)
        updatedOrderVersions2.forEach {
            assertThat(it.makeUsd).isNull()
            assertThat(it.takePriceUsd).isNull()
            assertThat(it.takeUsd).isEqualTo(newTakeUsd1)
            assertThat(it.makePriceUsd).isEqualTo(newMakePriceUsd1)
        }
    }

    private suspend fun save(vararg order: Order) {
        order.forEach { orderRepository.save(it) }
    }

    private suspend fun save(vararg orderVersion: OrderVersion) {
        orderVersion.forEach { orderVersionRepository.save(it).awaitFirst() }
    }
}
