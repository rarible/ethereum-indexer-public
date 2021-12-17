package com.rarible.protocol.order.listener.service.order

import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

@IntegrationTest
@FlowPreview
class OrderStatusUpdateTaskHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var orderStatusUpdateTaskHandler: OrderStatusUpdateTaskHandler

    @Test
    fun `should fix order status`() = runBlocking<Unit> {
        val order = createOrder()
        assertThat(order.status).isEqualTo(OrderStatus.ACTIVE)
        orderRepository.save(order)
        mongo.updateMulti(
            Query(),
            Update().set("status", OrderStatus.INACTIVE), MongoOrderRepository.COLLECTION
        ).awaitFirst()
        val orderCollection = mongo.getCollection(MongoOrderRepository.COLLECTION).awaitFirst()

        // If we try to read the order from the database, the status of it will be ACTIVE
        // because the 'status' field is calculated in constructor after the deserialization
        // But in the database that field is INACTIVE
        assertThat(orderRepository.findById(order.hash)?.status).isEqualTo(OrderStatus.ACTIVE)
        val rawOrderBefore = orderCollection.find().awaitSingle()
        assertThat(rawOrderBefore["status"]).isEqualTo("INACTIVE")

        orderStatusUpdateTaskHandler.runLongTask(null, "RARIBLE:INACTIVE").collect()

        assertThat(orderRepository.findById(order.hash)?.status).isEqualTo(OrderStatus.ACTIVE)
        val rawOrderAfter = orderCollection.find().awaitSingle()
        assertThat(rawOrderAfter["status"]).isEqualTo("ACTIVE")
    }
}
