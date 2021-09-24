package com.rarible.protocol.order.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.api.data.*
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.model.*
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.util.stream.Stream
import com.rarible.protocol.order.api.data.createOrder as createOrderFully

@IntegrationTest
class OrderSearchFt : AbstractIntegrationTest() {
    companion object {
        @JvmStatic
        fun orders(): Stream<Arguments> = run {
            val now = nowMillis()

            Stream.of(
                Arguments.of(
                    OrderSortDto.LAST_UPDATE_ASC,
                    listOf(
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(0)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(1)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(2)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(3)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(4))
                    )
                ),
                Arguments.of(
                    OrderSortDto.LAST_UPDATE_DESC,
                    listOf(
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(4)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(3)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(2)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(1)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(0))
                    )
                )
            )
        }
    }

    @ParameterizedTest
    @MethodSource("orders")
    fun `should find all orders with continuation`(
        sort: OrderSortDto,
        orders: List<Order>
    ) = runBlocking<Unit> {
        saveOrder(*orders.shuffled().toTypedArray())

        Wait.waitAssert {
            val allOrders = mutableListOf<OrderDto>()

            var continuation: String? = null
            do {
                val result = orderClient.getOrdersAllByStatus(sort, continuation, 2).awaitFirst()
                assertThat(result.orders).hasSizeLessThanOrEqualTo(2)

                allOrders.addAll(result.orders)
                continuation = result.continuation
            } while (continuation != null)

            assertThat(allOrders).hasSize(orders.size)

            allOrders.forEachIndexed { index, orderDto ->
                checkOrderDto(orderDto, orders[index])
            }
        }
    }

    @ParameterizedTest
    @MethodSource("orders")
    fun `should find all orders`(
        sort: OrderSortDto,
        orders: List<Order>
    ) = runBlocking<Unit> {
        saveOrder(*orders.shuffled().toTypedArray())

        Wait.waitAssert {
            val result = orderClient.getOrdersAllByStatus(sort, null, null).awaitFirst()
            assertThat(result.orders).hasSize(orders.size)

            result.orders.forEachIndexed { index, orderDto ->
                checkOrderDto(orderDto, orders[index])
            }
        }
    }

    private fun checkOrderDto(orderDto: OrderDto, order: Order) {
        assertThat(orderDto.hash).isEqualTo(order.hash)
    }

    private suspend fun saveOrder(vararg order: Order) {
        order.forEach { orderRepository.save(it) }
    }
}
