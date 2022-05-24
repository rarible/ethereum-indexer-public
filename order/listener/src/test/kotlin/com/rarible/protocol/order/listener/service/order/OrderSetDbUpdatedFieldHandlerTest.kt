package com.rarible.protocol.order.listener.service.order

import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
@FlowPreview
class OrderSetDbUpdatedFieldHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var handler: OrderSetDbUpdatedFieldHandler

    @Test
    fun `should update all orders without dbUpdatedAt field`() = runBlocking<Unit> {
        repeat(20) {
            val order = createOrder().copy(dbUpdatedAt = null)
            orderRepository.saveWithoutDbUpdated(order)
        }
        repeat(10) {
            val order = createOrder()
            orderRepository.saveWithoutDbUpdated(order.copy(dbUpdatedAt = order.lastUpdateAt))
        }
        assertThat(orderRepository.findWithoutDbUpdatedField().toList()).hasSize(20)

        handler.runLongTask(null, "").collect()

        assertThat(orderRepository.findWithoutDbUpdatedField().toList()).isEmpty()

        val orders = orderRepository.findAll().toList()
        assertThat(orders).hasSize(30)
        orders.forEach {
            assertThat(it.dbUpdatedAt).isEqualTo(it.lastUpdateAt)
        }
    }
}