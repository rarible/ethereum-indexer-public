package com.rarible.protocol.order.migration.integration.migration

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.migration.integration.AbstractMigrationTest
import com.rarible.protocol.order.migration.integration.IntegrationTest
import com.rarible.protocol.order.migration.mongock.mongo.ChangeLog00024AddHashToOrder
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import scalether.domain.AddressFactory

@IntegrationTest
internal class AddHashToOrderTest : AbstractMigrationTest() {

    private val migration = ChangeLog00024AddHashToOrder()

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var template: ReactiveMongoTemplate

    @Test
    fun `should set hash`() = runBlocking<Unit> {
        val hash = Word.apply("0xb50adc93b7327d6d0285588635d99047b2bae65ad18abb984441cf1ac5cacd41")
        val order = Order(
            id = Order.Id(hash),
            maker = AddressFactory.create(),
            taker = null,
            make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.ZERO),
            take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.ONE),
            makeStock = EthUInt256.ZERO,
            type = OrderType.RARIBLE_V2,
            fill = EthUInt256.ZERO,
            cancelled = false,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            lastUpdateAt = nowMillis()
        )
        orderRepository.save(order)

        // remove hash in DB
        template.updateFirst(
            Query(Criteria("_id").isEqualTo(order.id)),
            Update().set(Order::hash.name, null),
            MongoOrderRepository.COLLECTION
        ).awaitSingle()
        val dbOrderHash = getOrderHashFromDb(order.id)
        assertNull(dbOrderHash)

        migration.addHashFieldToOrder(template)

        val result = getOrderHashFromDb(order.id)
        assertEquals(hash, result)
    }

    private suspend fun getOrderHashFromDb(orderId: Order.Id): Word? = template.findOne(
        Query(Criteria("_id").isEqualTo(orderId)).also {
            it.fields().include(Order::id.name).include(Order::hash.name)
        },
        OrderIdAndHash::class.java,
        MongoOrderRepository.COLLECTION
    ).awaitSingle().hash

    private data class OrderIdAndHash(
        val _id: Order.Id,
        val hash: Word?
    )
}
