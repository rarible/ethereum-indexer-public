package com.rarible.protocol.order.listener.service.task

import com.rarible.core.test.data.randomBytes
import com.rarible.protocol.order.core.data.createOrderLooksrareDataV1
import com.rarible.protocol.order.core.data.createOrderLooksrareDataV2
import com.rarible.protocol.order.core.data.createOrderRaribleV1DataV3Sell
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@ExperimentalCoroutinesApi
@FlowPreview
@IntegrationTest
class CancelLooksrareV1OrdersTaskHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var handler: CancelLooksrareV1OrdersTaskHandler

    @Test
    fun `cancel LooksRare - ok`() = runBlocking<Unit> {
        val orderVersionToCancel = createOrderVersion().copy(
            type = OrderType.LOOKSRARE,
            platform = Platform.LOOKSRARE,
            data = createOrderLooksrareDataV1(),
            signature = Binary(randomBytes())
        )
        orderVersionRepository.save(orderVersionToCancel).awaitSingle()

        val orderToCancel = createOrder().copy(
            type = OrderType.LOOKSRARE,
            platform = Platform.LOOKSRARE,
            id = Order.Id(orderVersionToCancel.hash),
            data = createOrderLooksrareDataV1(),
            hash = orderVersionToCancel.hash,
            signature = Binary(randomBytes())
        )
        val order1 = createOrder().copy(
            type = OrderType.LOOKSRARE_V2,
            platform = Platform.LOOKSRARE,
            data = createOrderLooksrareDataV2(),
            signature = Binary(randomBytes())
        )
        val order2 = createOrder().copy(
            type = OrderType.RARIBLE_V2,
            platform = Platform.RARIBLE,
            data = createOrderRaribleV1DataV3Sell(),
            signature = Binary(randomBytes())
        )
        listOf(orderToCancel, order1, order2).forEach {
            val savedOrder = orderRepository.save(it)
            assertThat(savedOrder.status).isNotEqualTo(OrderStatus.CANCELLED)
        }

        handler.runLongTask(from = null, param = "").toList()

        listOf(orderToCancel).forEach {
            val savedOrder = orderRepository.findById(it.hash)
            assertThat(savedOrder?.status).isEqualTo(OrderStatus.CANCELLED)
            assertThat(savedOrder?.signature).isNull()
        }
        listOf(orderVersionToCancel).forEach {
            val savedOrderVersion = orderVersionRepository.findById(it.id).awaitSingle()
            assertThat(savedOrderVersion.signature).isNull()
        }

        listOf(order1, order2).forEach {
            val savedOrder = orderRepository.findById(it.hash)
            assertThat(savedOrder?.status).isNotEqualTo(OrderStatus.CANCELLED)
            assertThat(savedOrder?.signature).isNotNull
        }
    }
}