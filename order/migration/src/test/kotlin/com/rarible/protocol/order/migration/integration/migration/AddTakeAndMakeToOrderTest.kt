package com.rarible.protocol.order.migration.integration.migration

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.migration.integration.AbstractMigrationTest
import com.rarible.protocol.order.migration.integration.IntegrationTest
import com.rarible.protocol.order.migration.mongock.mongo.ChangeLog00012AddStatusToOrder
import com.rarible.protocol.order.migration.mongock.mongo.ChangeLog00013AddTakeMakeToOrder
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.util.stream.Stream

@IntegrationTest
class AddTakeAndMakeToOrderTest : AbstractMigrationTest() {

    val migration = ChangeLog00013AddTakeMakeToOrder()

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var orderVersionRepository: OrderVersionRepository

    @Autowired
    lateinit var template: ReactiveMongoTemplate

    @Autowired
    protected lateinit var orderUpdateService: OrderUpdateService

    @Autowired
    protected lateinit var priceUpdateService: PriceUpdateService

    @Test
    fun `should set prices for orders`() = runBlocking {
        val makeAddress = AddressFactory.create()
        val currencyToken = AddressFactory.create()
        val order1 = createOrder().copy(
            make = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.ONE),
            take = Asset(Erc20AssetType(currencyToken), EthUInt256.TEN)
        )
        orderRepository.save(order1)
        val order2 = createOrder().copy(
            make = Asset(Erc20AssetType(currencyToken), EthUInt256.TEN),
            take = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.ONE)
        )
        orderRepository.save(order2)

        // set prices to null
        template.updateMulti(Query(), Update().unset("makePrice"), MongoOrderRepository.COLLECTION).awaitFirst()
        template.updateMulti(Query(), Update().unset("takePrice"), MongoOrderRepository.COLLECTION).awaitFirst()

        migration.orders(priceUpdateService, template)
        var updatedOrder1 = orderRepository.findById(order1.hash)!!
        assertEquals(BigDecimal.valueOf(10L), updatedOrder1.makePrice)

        var updatedOrder2 = orderRepository.findById(order2.hash)!!
        assertEquals(BigDecimal.valueOf(10L), updatedOrder2.takePrice)
    }

    @Test
    fun `should set prices for order versions`() = runBlocking {
        val makeAddress = AddressFactory.create()
        val currencyToken = AddressFactory.create()
        val v1 = createOrderVersion(
            Asset(Erc20AssetType(currencyToken), EthUInt256.TEN),
            Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.ONE)
        )
        orderUpdateService.save(v1)

        migration.orderVersions(priceUpdateService, template)

        var updatedOrder1 = orderVersionRepository.findAll().collectList().awaitFirst()
        assertEquals(BigDecimal.valueOf(10L), updatedOrder1[0].takePrice)
    }

    private fun createOrder(
        maker: Address = AddressFactory.create(),
        taker: Address? = AddressFactory.create(),
        make: Asset = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
        start: Long? = null,
        end: Long? = null
    ): Order {
        return Order(
            maker = maker,
            taker = taker,
            make = make,
            take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5)),
            makeStock = make.value,
            type = OrderType.RARIBLE_V2,
            fill = EthUInt256.ZERO,
            cancelled = false,
            salt = EthUInt256.TEN,
            start = start,
            end = end,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            lastUpdateAt = nowMillis()
        )
    }

    private fun createOrderVersion(make: Asset, take: Asset) = OrderVersion(
        hash = Word.apply(RandomUtils.nextBytes(32)),
        maker = AddressFactory.create(),
        taker = AddressFactory.create(),
        makePriceUsd = (1..100).random().toBigDecimal(),
        takePriceUsd = (1..100).random().toBigDecimal(),
        makePrice = null,
        takePrice = null,
        makeUsd = (1..100).random().toBigDecimal(),
        takeUsd = (1..100).random().toBigDecimal(),
        make = make,
        take = take,
        platform = Platform.RARIBLE,
        type = OrderType.RARIBLE_V2,
        salt = EthUInt256.TEN,
        start = null,
        end = null,
        data = OrderRaribleV2DataV1(emptyList(), emptyList()),
        signature = null
    )
}
