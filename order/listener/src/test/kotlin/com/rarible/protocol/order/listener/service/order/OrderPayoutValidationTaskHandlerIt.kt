package com.rarible.protocol.order.listener.service.order

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class OrderPayoutValidationTaskHandlerIt {

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var handler: OrderPayoutValidationTaskHandler

    @Test
    fun `cancel orders - payouts are incorrect`() = runBlocking<Unit> {
        val order = orderRepository.save(createOrderWithPayouts(100))
        handler.runLongTask(null, "").collect()

        val cancelled = orderRepository.findById(order.hash)!!
        assertThat(cancelled.status).isEqualTo(OrderStatus.CANCELLED)
    }

    @Test
    fun `skip orders - payouts are correct`() = runBlocking<Unit> {
        // 10000 - OK
        val order1 = orderRepository.save(createOrderWithPayouts(2000, 8000))
        // Empty - OK
        val order2 = orderRepository.save(createOrderWithPayouts())
        handler.runLongTask(null, "").collect()

        val skipped1 = orderRepository.findById(order1.hash)!!
        val skipped2 = orderRepository.findById(order2.hash)!!
        assertThat(skipped1).isEqualTo(order1)
        assertThat(skipped2).isEqualTo(order2)
    }

    @Test
    fun `skip orders - already cancelled`() = runBlocking<Unit> {
        val order = orderRepository.save(createOrderWithPayouts(2000).withCancel(true))
        handler.runLongTask(null, "").collect()

        val skipped = orderRepository.findById(order.hash)!!
        assertThat(skipped).isEqualTo(order)
    }

    @Test
    fun `skip orders - not rarible`() = runBlocking<Unit> {
        val order = orderRepository.save(createOrderWithPayouts(2000).copy(platform = Platform.X2Y2))
        handler.runLongTask(null, "").collect()

        val skipped = orderRepository.findById(order.hash)!!
        assertThat(skipped).isEqualTo(order)
    }

    private fun createOrderWithPayouts(vararg payout: Int): Order {
        val parts = payout.map {
            Part(
                randomAddress(),
                EthUInt256(it.toBigInteger())
            )
        }
        val data = OrderRaribleV2DataV1(parts, emptyList())
        return createOrder().copy(data = data)
    }

}