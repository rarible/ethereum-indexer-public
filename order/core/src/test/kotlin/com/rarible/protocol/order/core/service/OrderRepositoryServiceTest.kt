package com.rarible.protocol.order.core.service

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.OrderFilterAllDto
import com.rarible.protocol.dto.OrderFilterDto
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import com.rarible.protocol.order.core.model.Order
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
            val filter = OrderFilterAllDto(sort = OrderFilterDto.Sort.LAST_UPDATE_DESC)
            val collectedOrders = mutableListOf<Order>()

            orderRepositoryService.search(filter, 3).collect {
                collectedOrders.addAll(it)
            }
            assertThat(collectedOrders.size).isEqualTo(orders.size)
            assertThat(collectedOrders).containsExactlyInAnyOrder(*orders.toTypedArray())
        }
    }
}
