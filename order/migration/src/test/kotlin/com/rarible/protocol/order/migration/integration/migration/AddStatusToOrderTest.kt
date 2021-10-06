package com.rarible.protocol.order.migration.integration.migration

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.migration.integration.AbstractMigrationTest
import com.rarible.protocol.order.migration.integration.IntegrationTest
import com.rarible.protocol.order.migration.mongock.mongo.ChangeLog00012AddStatusToOrder
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import scalether.domain.AddressFactory
import java.util.stream.Stream

@IntegrationTest
class AddStatusToOrderTest : AbstractMigrationTest() {

    val migration = ChangeLog00012AddStatusToOrder()

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var template: ReactiveMongoTemplate

    companion object {
        @JvmStatic
        fun orders(): Stream<Arguments> = run {
            val order = Order(
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
            Stream.of(
                Arguments.of(order, OrderStatus.INACTIVE),
                Arguments.of(order.copy(makeStock = EthUInt256.ONE), OrderStatus.ACTIVE),
                Arguments.of(order.copy(fill = EthUInt256.TEN, take = Asset(EthAssetType, EthUInt256.TEN)), OrderStatus.FILLED),
                Arguments.of(order.copy(cancelled = true), OrderStatus.CANCELLED)
            )
        }
    }

    @ParameterizedTest
    @MethodSource("orders")
    fun `should set status`(order: Order, status: OrderStatus) = runBlocking {
        orderRepository.save(order)

        // remove status
        template.updateMulti(Query(), Update().unset("status"), MongoOrderRepository.COLLECTION).awaitFirst()

        migration.migrate(template)
        val result = orderRepository.findById(order.hash)
        assertEquals(status, result?.status)
    }
}
