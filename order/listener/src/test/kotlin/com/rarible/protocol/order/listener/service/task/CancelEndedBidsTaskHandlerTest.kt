package com.rarible.protocol.order.listener.service.task

import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.listener.data.createBidOrderVersion
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
import java.time.Duration
import java.time.Instant

@ExperimentalCoroutinesApi
@FlowPreview
@IntegrationTest
class CancelEndedBidsTaskHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var handler: CancelEndedBidsTaskHandler

    @Test
    internal fun `cancel ended bids`() = runBlocking<Unit> {
        val now = Instant.now()

        val orderVersion1 = createBidOrderVersion().copy(end = (now - Duration.ofDays(1)).epochSecond)
        orderVersionRepository.save(orderVersion1).awaitSingle()

        val expiredOrder1 = createOrderBid().copy(
            id = Order.Id(orderVersion1.hash),
            hash = orderVersion1.hash,
            end = (now - Duration.ofDays(1)).epochSecond
        )
        val expiredOrder2 = createOrderBid().copy(end = (now - Duration.ofDays(1)).epochSecond)
        val notExpiredOrder = createOrderBid().copy(end = (now + Duration.ofDays(1)).epochSecond)
        listOf(expiredOrder1, expiredOrder2).forEach {
            val savedOrder = orderRepository.save(it)
            assertThat(savedOrder.status).isEqualTo(OrderStatus.ENDED)
        }
        listOf(notExpiredOrder).forEach {
            val savedOrder = orderRepository.save(it)
            assertThat(savedOrder.status).isNotEqualTo(OrderStatus.ENDED)
        }
        val count = handler.runLongTask(null, "").toList()
        assertThat(count).hasSize(3)

        val updatedOrder1 = orderRepository.findById(expiredOrder1.hash)!!
        assertThat(updatedOrder1.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(updatedOrder1.cancelled).isTrue

        val updatedOrder2 = orderRepository.findById(expiredOrder2.hash)!!
        assertThat(updatedOrder2.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(updatedOrder2.cancelled).isTrue

        val updatedUotExpiredOrder = orderRepository.findById(notExpiredOrder.hash)!!
        assertThat(updatedUotExpiredOrder.status).isNotEqualTo(OrderStatus.CANCELLED)
    }
}
