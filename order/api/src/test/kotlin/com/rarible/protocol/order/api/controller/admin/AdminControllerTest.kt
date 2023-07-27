package com.rarible.protocol.order.api.controller.admin

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderStateDto
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.api.service.order.OrderService
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.web.client.RestTemplate

@IntegrationTest
internal class AdminControllerTest : AbstractIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    lateinit var orderService: OrderService

    @Autowired
    lateinit var orderStateRepository: OrderStateRepository

    @Autowired
    lateinit var restTemplate: RestTemplate

    @Test
    @Suppress("BlockingMethodInNonBlockingContext")
    fun `should cancel order`() = runBlocking<Unit> {
        val orderVersion = createOrderVersion()
        val savedOrder = save(orderVersion)
        assertThat(savedOrder.status).isNotEqualTo(OrderStatus.CANCELLED)

        restTemplate.postForEntity(
            "http://localhost:$port/admin/order/orders/${savedOrder.hash.prefixed()}/state",
            OrderStateDto(canceled = true),
            OrderDto::class.java
        )

        val updatedOrder = orderService.get(savedOrder.hash)
        assertThat(updatedOrder.status).isEqualTo(OrderStatus.CANCELLED)
    }

    @Test
    @Suppress("BlockingMethodInNonBlockingContext")
    fun `should change cancel state`() = runBlocking<Unit> {
        val orderVersion = createOrderVersion()
        orderStateRepository.save(OrderState(orderVersion.hash, canceled = true))
        val savedOrder = save(orderVersion)
        assertThat(savedOrder.status).isEqualTo(OrderStatus.CANCELLED)

        restTemplate.postForEntity(
            "http://localhost:$port/admin/order/orders/${savedOrder.hash.prefixed()}/state",
            OrderStateDto(canceled = false),
            OrderDto::class.java
        )

        val updatedOrder = orderService.get(savedOrder.hash)
        assertThat(updatedOrder.status).isNotEqualTo(OrderStatus.CANCELLED)
    }
}
