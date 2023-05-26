package com.rarible.protocol.order.listener.service.task

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
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
            end = Instant.now().epochSecond
        )
        val notExpiredOrder2 = createOrderBid().copy(
            end = null
        )
        listOf(expiredOrder1, expiredOrder2).forEach {
            val savedOrder = orderRepository.save(it)
            Assertions.assertThat(savedOrder.status).isNotEqualTo(OrderStatus.CANCELLED)
        }
        listOf(notExpiredOrder1, notExpiredOrder2).forEach {
            val savedOrder = orderRepository.save(it)
            Assertions.assertThat(savedOrder.status).isNotEqualTo(OrderStatus.CANCELLED)
        }
        val count = handler.runLongTask(null, "").toList()
        Assertions.assertThat(count).hasSize(3)

        val updatedOrder1 = orderRepository.findById(expiredOrder1.hash)!!
        Assertions.assertThat(updatedOrder1.status).isEqualTo(OrderStatus.CANCELLED)
        Assertions.assertThat(updatedOrder1.cancelled).isTrue

        val updatedOrder2 = orderRepository.findById(expiredOrder2.hash)!!
        Assertions.assertThat(updatedOrder2.status).isEqualTo(OrderStatus.CANCELLED)
        Assertions.assertThat(updatedOrder2.cancelled).isTrue

        val updatedUotExpiredOrder1 = orderRepository.findById(notExpiredOrder1.hash)!!
        Assertions.assertThat(updatedUotExpiredOrder1.status).isNotEqualTo(OrderStatus.CANCELLED)

        val updatedUotExpiredOrder2 = orderRepository.findById(notExpiredOrder2.hash)!!
        Assertions.assertThat(updatedUotExpiredOrder2.status).isNotEqualTo(OrderStatus.CANCELLED)
    }
}
