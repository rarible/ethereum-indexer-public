package com.rarible.protocol.order.listener.service.task

import com.rarible.core.common.nowMillis
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.listener.data.createBidOrderVersion
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.data.createOrderBid
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.temporal.ChronoUnit

@ExperimentalCoroutinesApi
@FlowPreview
@IntegrationTest
class CancelOrdersWithoutExpiredTimeTaskHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var handler: CancelOrdersWithoutExpiredTimeTaskHandler

    @Autowired
    private lateinit var properties: OrderIndexerProperties

    @Test
    internal fun `cancel old bids with no ended`() = runBlocking<Unit> {
        val orderVersion1 = createBidOrderVersion().copy(
            end = null,
            createdAt = properties.raribleOrderExpiration.fixedExpireDate
        )
        orderVersionRepository.save(orderVersion1).awaitSingle()

        val expiredOrder1 = createOrderBid().copy(
            id = Order.Id(orderVersion1.hash),
            hash = orderVersion1.hash,
            end = null,
            createdAt = properties.raribleOrderExpiration.fixedExpireDate
        )
        val expiredOrder2 = createOrderBid().copy(
            end = null,
            createdAt = properties.raribleOrderExpiration.fixedExpireDate
        )
        val notExpiredOrder1 = createOrderBid().copy(
            end = nowMillis().epochSecond
        )
        val expiredOrder3 = createOrderBid().copy(
            end = null
        )
        val expiredOrder4 = createOrder().copy(
            end = null,
            approved = false,
        )
        val notExpiredOrder5 = createOrder().copy(
            end = null,
        )
        val notExpiredOrder6 = createOrder().copy(
            end = nowMillis().plus(7, ChronoUnit.DAYS).epochSecond,
            approved = false
        )
        listOf(
            expiredOrder1,
            expiredOrder2,
            expiredOrder3,
            notExpiredOrder1,
            expiredOrder4,
            notExpiredOrder5,
            notExpiredOrder6
        ).forEach {
            val savedOrder = orderRepository.save(it)
            assertThat(savedOrder.status).isNotEqualTo(OrderStatus.CANCELLED)
        }
        handler.runLongTask(null, "").toList()

        listOf(expiredOrder1, expiredOrder2, expiredOrder3, expiredOrder4).forEach {
            val updatedOrder = orderRepository.findById(it.hash)!!
            assertThat(updatedOrder.status).isEqualTo(OrderStatus.CANCELLED)
            assertThat(updatedOrder.cancelled).isTrue
        }

        listOf(notExpiredOrder1, notExpiredOrder5, notExpiredOrder6).forEach {
            val updatedUotExpiredOrder = orderRepository.findById(it.hash)!!
            assertThat(updatedUotExpiredOrder.status).isNotEqualTo(OrderStatus.CANCELLED)
        }
    }
}
