package com.rarible.protocol.order.core.repository.order

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.TestPropertiesConfiguration
import com.rarible.protocol.order.core.configuration.RepositoryConfiguration
import com.rarible.protocol.order.core.data.createBidOrder
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.data.createOrderBasicSeaportDataV1
import com.rarible.protocol.order.core.data.createOrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.data.createOrderRaribleV2DataV1
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import scalether.domain.Address
import java.time.Duration
import java.time.Instant

@MongoTest
@MongoCleanup
@DataMongoTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [RepositoryConfiguration::class, TestPropertiesConfiguration::class])
@ActiveProfiles("integration")
internal class OrderRepositoryIt {

    @Autowired
    private lateinit var mongo: ReactiveMongoTemplate

    @Autowired
    private lateinit var orderRepository: OrderRepository

    // Simple Order repository
    private lateinit var delegate: OrderRepository

    @BeforeEach
    fun beforeEach() = runBlocking<Unit> {
        delegate = MongoOrderRepository(mongo)
        delegate.createIndexes()
    }

    @Test
    fun `test order raw format`() = runBlocking<Unit> {
        val order = createOrder()

        delegate.save(order)

        val document = mongo.findById(
            order.id,
            Document::class.java,
            MongoOrderRepository.COLLECTION
        ).block()

        assertEquals(
            order.maker,
            Address.apply(document.getString(Order::maker.name))
        )
        assertEquals(
            order.createdAt.toEpochMilli(),
            document.getDate(Order::createdAt.name).time
        )
        assertEquals(
            order.fill,
            EthUInt256.Companion.of(document.getString(Order::fill.name))
        )
    }

    @Test
    fun `should remove order`() = runBlocking {
        val hash = delegate.save(createOrder()).hash

        val savedOrder = delegate.findById(hash)
        assertThat(savedOrder).isNotNull

        orderRepository.remove(hash)

        val removedOrder = delegate.findById(hash)
        assertThat(removedOrder).isNull()
    }

    @Test
    fun `find all maker OpenSea hashes`() = runBlocking<Unit> {
        val maker = randomAddress()
        val currentNonce = 0L
        val newNonce = 1L
        val order1 = createOrder().copy(
            maker = maker,
            platform = Platform.OPEN_SEA,
            data = createOrderOpenSeaV1DataV1().copy(nonce = currentNonce)
        )
        val order2 = createOrder().copy(
            maker = maker,
            platform = Platform.OPEN_SEA,
            data = createOrderOpenSeaV1DataV1().copy(nonce = currentNonce)
        )
        val order3 = createOrder().copy(
            maker = maker,
            platform = Platform.OPEN_SEA,
            data = createOrderOpenSeaV1DataV1().copy(nonce = newNonce)
        )
        val order4 = createOrder().copy(
            maker = maker,
            platform = Platform.RARIBLE,
            data = createOrderOpenSeaV1DataV1().copy(nonce = currentNonce)
        )
        val order5 = createOrder().copy(
            maker = randomAddress(),
            platform = Platform.OPEN_SEA,
            data = createOrderOpenSeaV1DataV1().copy(nonce = currentNonce)
        )
        val order6 = createOrder().copy(
            maker = randomAddress(),
            platform = Platform.OPEN_SEA,
            data = createOrderOpenSeaV1DataV1().copy(nonce = newNonce)
        )
        listOf(order1, order2, order3, order4, order5, order6).forEach {
            delegate.save(it)
        }
        val hashes = delegate.findOpenSeaHashesByMakerAndByNonce(maker, currentNonce, newNonce).toList()
        assertThat(hashes).containsExactlyInAnyOrder(order1.hash, order2.hash)
    }

    @Test
    fun `find all maker Seaport hashes by counter`() = runBlocking<Unit> {
        val maker = randomAddress()
        val counter = 0L
        val newCounter = 1L
        val order0 = createOrder().copy(
            maker = maker,
            cancelled = true,
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1().copy(counter = counter)
        )
        val order1 = createOrder().copy(
            maker = maker,
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1().copy(counter = counter)
        )
        val order2 = createOrder().copy(
            maker = maker,
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1().copy(counter = counter)
        )
        val order3 = createOrder().copy(
            maker = maker,
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1().copy(counter = newCounter)
        )
        val order4 = createOrder().copy(
            maker = maker,
            platform = Platform.RARIBLE,
            data = createOrderRaribleV2DataV1()
        )
        val order5 = createOrder().copy(
            maker = randomAddress(),
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1().copy(counter = counter)
        )
        val order6 = createOrder().copy(
            maker = randomAddress(),
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1().copy(counter = newCounter)
        )
        listOf(order0, order1, order2, order3, order4, order5, order6).forEach {
            delegate.save(it)
        }
        val hashes = delegate.findNotCanceledByMakerAndByCounter(maker, counter).toList()
        assertThat(hashes).containsExactlyInAnyOrder(order1.hash, order2.hash)
    }

    @Test
    fun `find all maker order by counters`() = runBlocking<Unit> {
        val maker = randomAddress()
        val counter1 = 0L
        val counter2 = 1L
        val otherCounter = 2L
        val order0 = createOrder().copy(
            maker = maker,
            cancelled = true,
            platform = Platform.LOOKSRARE,
            data = createOrderBasicSeaportDataV1().copy(counter = counter1)
        )
        val order1 = createOrder().copy(
            maker = maker,
            cancelled = true,
            platform = Platform.LOOKSRARE,
            data = createOrderBasicSeaportDataV1().copy(counter = counter2)
        )
        val order2 = createOrder().copy(
            maker = maker,
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1().copy(counter = counter1)
        )
        val order3 = createOrder().copy(
            maker = maker,
            platform = Platform.LOOKSRARE,
            data = createOrderBasicSeaportDataV1().copy(counter = otherCounter)
        )
        val order4 = createOrder().copy(
            maker = maker,
            platform = Platform.RARIBLE,
            data = createOrderRaribleV2DataV1()
        )
        val order5 = createOrder().copy(
            maker = randomAddress(),
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1().copy(counter = counter1)
        )
        val order6 = createOrder().copy(
            maker = randomAddress(),
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1().copy(counter = counter1)
        )
        listOf(order0, order1, order2, order3, order4, order5, order6).forEach {
            delegate.save(it)
        }
        val hashes = delegate.findByMakeAndByCounters(Platform.LOOKSRARE, maker, listOf(counter1, counter2)).map { it.id }.toList()
        assertThat(hashes).containsExactlyInAnyOrder(order0.id, order1.id)
    }

    @Test
    fun `find maker order by counter`() = runBlocking<Unit> {
        val maker = randomAddress()
        val counter = 100L
        val otherCounter = 2L
        val order0 = createOrder().copy(
            maker = maker,
            cancelled = true,
            platform = Platform.LOOKSRARE,
            data = createOrderBasicSeaportDataV1().copy(counter = counter)
        )
        val order1 = createOrder().copy(
            maker = maker,
            cancelled = true,
            platform = Platform.LOOKSRARE,
            data = createOrderBasicSeaportDataV1().copy(counter = otherCounter)
        )
        val order2 = createOrder().copy(
            maker = maker,
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1().copy(counter = counter)
        )
        listOf(order0, order1, order2).forEach {
            delegate.save(it)
        }
        val hashes = delegate.findByMakeAndByCounters(Platform.LOOKSRARE, maker, listOf(counter)).map { it.id }.toList()
        assertThat(hashes).containsExactlyInAnyOrder(order0.id)
    }

    @Test
    fun `should find all sell currencies by collection`() = runBlocking<Unit> {
        val token = randomAddress()
        val currencies = listOf(randomAddress(), randomAddress(), randomAddress())

        //Active orders
        repeat(10) {
            currencies.forEach { currency ->
                val order = createOrder().copy(make = randomErc721(token), take = randomErc20(currency))
                val savedOrder = orderRepository.save(order)
                assertThat(savedOrder.status).isEqualTo(OrderStatus.ACTIVE)
            }
        }
        //Not active orders
        repeat(10) {
            currencies.forEach { currency ->
                val order = createOrder().copy(make = randomErc721(token), take = randomErc20(randomAddress()), cancelled = true)
                val savedOrder = orderRepository.save(order)
                assertThat(savedOrder.status).isNotEqualTo(OrderStatus.ACTIVE)
            }
        }
        val foundCurrencies = orderRepository.findActiveSellCurrenciesByCollection(token)
        assertThat(foundCurrencies).containsExactlyInAnyOrderElementsOf(currencies)
    }

    @Test
    fun `find - before target last update`() = runBlocking<Unit> {
        val before = Instant.now() - Duration.ofMinutes(1)
        val order1 = createBidOrder().copy(lastUpdateAt = before - Duration.ofMinutes(1))
        val order2 = createBidOrder().copy(lastUpdateAt = before - Duration.ofMinutes(2))
        val order3 = createBidOrder().copy(lastUpdateAt = before + Duration.ofMinutes(1))
        val order4 = createBidOrder().copy(lastUpdateAt = before + Duration.ofMinutes(2))
        listOf(order1, order2, order3, order4).forEach { orderRepository.save(it) }
        val bids = orderRepository.findAllLiveBidsHashesLastUpdatedBefore(before).toList()
        assertThat(bids).containsExactlyInAnyOrder(order1.hash, order2.hash)
    }
}

