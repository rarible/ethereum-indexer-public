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
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@ExperimentalCoroutinesApi
@FlowPreview
@IntegrationTest
class CancelBidsWithoutExpiredTimeTaskHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var handler: CancelBidsWithoutExpiredTimeTaskHandler

    @Test
    internal fun `cancel bids with no ended`() = runBlocking<Unit> {
        val orderVersion1 = createBidOrderVersion().copy(end = null)
        orderVersionRepository.save(orderVersion1).awaitSingle()

        val expiredOrder1 = createOrderBid().copy(
            id = Order.Id(orderVersion1.hash),
            hash = orderVersion1.hash,
            end = null
        )
        val expiredOrder2 = createOrderBid().copy(end = null)
        val notExpiredOrder = createOrderBid().copy(end = Instant.now().epochSecond)

        listOf(expiredOrder1, expiredOrder2).forEach {
            val savedOrder = orderRepository.save(it)
            Assertions.assertThat(savedOrder.status).isNotEqualTo(OrderStatus.CANCELLED)
        }
        listOf(notExpiredOrder).forEach {
            val savedOrder = orderRepository.save(it)
            Assertions.assertThat(savedOrder.status).isNotEqualTo(OrderStatus.CANCELLED)
        }
        val count = handler.runLongTask(null, "").toList()
        Assertions.assertThat(count).hasSize(2)

        val updatedOrder1 = orderRepository.findById(expiredOrder1.hash)!!
        Assertions.assertThat(updatedOrder1.status).isEqualTo(OrderStatus.CANCELLED)
        Assertions.assertThat(updatedOrder1.cancelled).isTrue

        val updatedOrder2 = orderRepository.findById(expiredOrder2.hash)!!
        Assertions.assertThat(updatedOrder2.status).isEqualTo(OrderStatus.CANCELLED)
        Assertions.assertThat(updatedOrder2.cancelled).isTrue

        val updatedUotExpiredOrder = orderRepository.findById(notExpiredOrder.hash)!!
        Assertions.assertThat(updatedUotExpiredOrder.status).isNotEqualTo(OrderStatus.CANCELLED)
    }
}
