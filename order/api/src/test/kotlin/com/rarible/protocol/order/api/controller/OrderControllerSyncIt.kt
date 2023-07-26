package com.rarible.protocol.order.api.controller

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.SyncSortDto
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.data.randomOrder
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class OrderControllerSyncIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var controller: OrderController

    @Test
    fun `should get all orders using pagination desc`() = runBlocking<Unit> {
        val ordersQuantity = 30
        val ordersChunk = 5

        repeat(ordersQuantity) {
            orderRepository.save(randomOrder())
        }

        var continuation: String? = null
        var pageCounter = 0
        val receivedOrders = mutableListOf<OrderDto>()

        do {
            val dto = controller.getAllSync(SyncSortDto.DB_UPDATE_DESC, continuation, ordersChunk)
            continuation = dto.body?.continuation
            dto.body?.let { receivedOrders.addAll(it.orders) }
            pageCounter += 1
        } while (continuation != null)

        assertThat(pageCounter).isEqualTo(ordersQuantity / ordersChunk + 1)
        assertThat(receivedOrders).hasSize(ordersQuantity)
        assertThat(receivedOrders).isSortedAccordingTo { o1, o2 -> o2.dbUpdatedAt!!.compareTo(o1.dbUpdatedAt) }
    }

    @Test
    fun `should get all orders using pagination asc`() = runBlocking<Unit> {
        val ordersQuantity = 35
        val ordersChunk = 9

        repeat(ordersQuantity) {
            orderRepository.save(randomOrder())
        }

        var continuation: String? = null
        var pageCounter = 0
        val receivedOrders = mutableListOf<OrderDto>()

        do {
            val dto = controller.getAllSync(SyncSortDto.DB_UPDATE_ASC, continuation, ordersChunk)
            continuation = dto.body?.continuation
            dto.body?.let { receivedOrders.addAll(it.orders) }
            pageCounter += 1
        } while (continuation != null)

        assertThat(pageCounter).isEqualTo(ordersQuantity / ordersChunk + 1)
        assertThat(receivedOrders).hasSize(ordersQuantity)
        assertThat(receivedOrders).isSortedAccordingTo { o1, o2 -> o1.dbUpdatedAt!!.compareTo(o2.dbUpdatedAt) }
    }
}
