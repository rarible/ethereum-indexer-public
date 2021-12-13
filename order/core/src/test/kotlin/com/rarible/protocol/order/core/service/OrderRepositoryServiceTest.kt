package com.rarible.protocol.order.core.service

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderFilter
import com.rarible.protocol.order.core.model.OrderFilterAll
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class OrderRepositoryServiceTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var orderRepositoryService: OrderRepositoryService

    @Test
    fun `should get all order by target batches`() = runBlocking {
        val orders = (1..10).map {
            orderRepository.save(createOrder())
        }
        Wait.waitAssert {
            val filter = OrderFilterAll(sort = OrderFilter.Sort.LAST_UPDATE_DESC, platforms = emptyList())
            val collectedOrders = mutableListOf<Order>()

            orderRepositoryService.search(filter, 3).collect {
                collectedOrders.addAll(it)
            }
            assertThat(collectedOrders.size).isEqualTo(orders.size)
            assertThat(collectedOrders).containsExactlyInAnyOrder(*orders.toTypedArray())
        }
    }
}
