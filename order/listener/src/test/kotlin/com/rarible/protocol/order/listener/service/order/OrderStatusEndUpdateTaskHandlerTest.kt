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
import java.time.Instant.now

@IntegrationTest
@FlowPreview
class OrderStatusEndUpdateTaskHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var orderStatusEndUpdateTaskHandler: OrderStatusEndUpdateTaskHandler

    @Test
    fun `should fix order status`() = runBlocking<Unit> {
        val order = createOrder().copy(end = 0, start = now().minusSeconds(10).epochSecond)
        assertThat(order.status).isEqualTo(OrderStatus.ACTIVE)
        orderRepository.save(order)
        mongo.updateMulti(
            Query(),
            Update().set("status", OrderStatus.ENDED), MongoOrderRepository.COLLECTION
        ).awaitFirst()
        val orderCollection = mongo.getCollection(MongoOrderRepository.COLLECTION).awaitFirst()

        val rawOrderBefore = orderCollection.find().awaitSingle()
        assertThat(rawOrderBefore["status"]).isEqualTo("ENDED")

        orderStatusEndUpdateTaskHandler.runLongTask(null, "").collect()

        assertThat(orderRepository.findById(order.hash)?.status).isEqualTo(OrderStatus.ACTIVE)
        val rawOrderAfter = orderCollection.find().awaitSingle()
        assertThat(rawOrderAfter["status"]).isEqualTo("ACTIVE")
    }
}
