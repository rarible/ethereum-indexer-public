package com.rarible.protocol.order.listener.service.order

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.data.createOrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@FlowPreview
@IntegrationTest
class RemoveOpenSeaOutdatedOrdersTaskHandlerTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var removeOpenSeaOutdatedOrdersTaskHandler:RemoveOpenSeaOutdatedOrdersTaskHandler

    @Test
    fun `remove OpenSea orders`() = runBlocking<Unit> {
        exchangeContractAddresses.openSeaV1 = randomAddress()
        exchangeContractAddresses.openSeaV2 = randomAddress()

        repeat(3) {
            orderRepository.save(createOrder().copy(status = OrderStatus.ACTIVE)).hash
        }

        repeat(3) {
            orderRepository.save(createOrder().copy(status = OrderStatus.CANCELLED))
        }

        repeat(5) {
            val orderVersion = createOrderVersion().copy(
                platform = Platform.OPEN_SEA,
                data = createOrderOpenSeaV1DataV1().copy(exchange = exchangeContractAddresses.openSeaV1, nonce = null),
                type = OrderType.OPEN_SEA_V1,
                createdAt = Instant.EPOCH,
            )
            orderVersionRepository.save(orderVersion).awaitFirst()
            val order = orderVersion.toOrderExactFields().copy(
                status = arrayOf(OrderStatus.ACTIVE, OrderStatus.INACTIVE, OrderStatus.NOT_STARTED).random()
            )
            orderRepository.save(order)
        }

        val updatedOrdersHashes = removeOpenSeaOutdatedOrdersTaskHandler.runLongTask(null, "").toList()
        val updatedOrdersFromRep = orderRepository.findAll(updatedOrdersHashes.map{Word.apply(it)}).toList()

        assertEquals(5, updatedOrdersFromRep.count())
        updatedOrdersFromRep.forEach { assertEquals(true, it.cancelled) }
        updatedOrdersFromRep.forEach { assertEquals(OrderStatus.CANCELLED, it.status) }
        updatedOrdersFromRep.forEach { assertEquals(OrderType.OPEN_SEA_V1, it.type) }
        updatedOrdersFromRep.forEach { assertEquals(Instant.ofEpochSecond(1645812000), it.lastUpdateAt) }
        updatedOrdersFromRep.forEach { assertEquals(exchangeContractAddresses.openSeaV1, (it.data as OrderOpenSeaV1DataV1).exchange) }
    }
}