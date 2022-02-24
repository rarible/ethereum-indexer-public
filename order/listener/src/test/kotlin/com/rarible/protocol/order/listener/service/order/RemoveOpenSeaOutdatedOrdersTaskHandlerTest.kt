package com.rarible.protocol.order.listener.service.order

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

@FlowPreview
@IntegrationTest
class RemoveOpenSeaOutdatedOrdersTaskHandlerTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var removeOpenSeaOutdatedOrdersTaskHandler:RemoveOpenSeaOutdatedOrdersTaskHandler

    @Test
    fun `remove OpenSea orders`() = runBlocking<Unit> {
        repeat(3) {
            orderRepository.save(createOrder().copy(status = OrderStatus.ACTIVE)).hash
        }

        repeat(3) {
            orderRepository.save(createOrder().copy(status = OrderStatus.CANCELLED))
        }

        repeat(5) {
            val orderVersion = createOrderVersion().copy(
                platform = Platform.OPEN_SEA,
                data = createOrderOpenSeaV1DataV1().copy(exchange = exchangeContractAddresses.openSeaV1),
                type = OrderType.OPEN_SEA_V1
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
        updatedOrdersFromRep.forEach{assertEquals(true, it.cancelled)}
        updatedOrdersFromRep.forEach{assertEquals(OrderStatus.CANCELLED, it.status)}
    }
}