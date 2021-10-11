package com.rarible.protocol.order.migration.integration.migration

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.migration.integration.AbstractMigrationTest
import com.rarible.protocol.order.migration.integration.IntegrationTest
import com.rarible.protocol.order.migration.mongock.mongo.ChangeLog00013AddTakeMakeToOrder
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigDecimal

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

    @Test
    fun `should set prices for orders`() = runBlocking {
        val makeAddress = AddressFactory.create()
        val currencyToken = AddressFactory.create()
        val order1 = createOrder().copy(
            make = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            take = Asset(Erc20AssetType(currencyToken), EthUInt256.of(11))
        )
        orderRepository.save(order1)
        val order2 = createOrder().copy(
            make = Asset(Erc20AssetType(currencyToken), EthUInt256.of(12)),
            take = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN)
        )
        orderRepository.save(order2)

        // set prices to null
        template.updateMulti(Query(), Update().unset("makePrice"), MongoOrderRepository.COLLECTION).awaitFirst()
        template.updateMulti(Query(), Update().unset("takePrice"), MongoOrderRepository.COLLECTION).awaitFirst()

        migration.orders(template)
        var updatedOrder1 = orderRepository.findById(order1.hash)!!
        assertEquals(BigDecimal.valueOf(11L), updatedOrder1.makePrice)

        var updatedOrder2 = orderRepository.findById(order2.hash)!!
        assertEquals(BigDecimal.valueOf(12L), updatedOrder2.takePrice)
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
}
