package com.rarible.protocol.order.listener.service.order

import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
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
            orderRepository.saveWithoutDbUpdated(createOrder())
        }

        repeat(10) {
            val order = createOrder()
            orderRepository.saveWithoutDbUpdated(order.copy(dbUpdatedAt = order.lastUpdateAt))
        }

        Assertions.assertThat(orderRepository.findWithoutDbUpdatedField().toList()).hasSize(20)

        handler.runLongTask(null, "").collect()

        Assertions.assertThat(orderRepository.findWithoutDbUpdatedField().toList()).isEmpty()

        Assertions.assertThat(orderRepository.findAll().toList()).hasSize(30)

        orderRepository.findAll().map {Assertions.assertThat(it.lastUpdateAt).isEqualTo(it.dbUpdatedAt)}.collect()
    }
}