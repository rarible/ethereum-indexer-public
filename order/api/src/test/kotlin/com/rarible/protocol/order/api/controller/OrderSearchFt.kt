package com.rarible.protocol.order.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.api.data.*
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.model.*
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.AddressFactory
import java.time.*
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

        @JvmStatic
        fun orders4All(): Stream<Arguments> = run {
            val order: () -> Order = { createOrderFully().copy(makeStock = EthUInt256.ZERO) }
            val inactiveOrder = order().withCurrentStatus()
            val activeOrder = order().copy(makeStock = EthUInt256.ONE).withCurrentStatus()
            val filledOrder = order().copy(fill = EthUInt256.TEN, take = Asset(EthAssetType, EthUInt256.TEN)).withCurrentStatus()
            val canceledOrder = order().copy(cancelled = true).withCurrentStatus()
            val orders = listOf(inactiveOrder, activeOrder, filledOrder, canceledOrder)
            Stream.of(
                Arguments.of(orders, inactiveOrder, listOf(OrderStatusDto.INACTIVE)),
                Arguments.of(orders, activeOrder, listOf(OrderStatusDto.ACTIVE)),
                Arguments.of(orders, filledOrder, listOf(OrderStatusDto.FILLED)),
                Arguments.of(orders, canceledOrder, listOf(OrderStatusDto.CANCELLED))
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
                val result = orderClient.getOrdersAllByStatus(sort, continuation, 2, null).awaitFirst()
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
            val result = orderClient.getOrdersAllByStatus(sort, null, null, null).awaitFirst()
            assertThat(result.orders).hasSize(orders.size)

            result.orders.forEachIndexed { index, orderDto ->
                checkOrderDto(orderDto, orders[index])
            }
        }
    }

    @ParameterizedTest
    @MethodSource("orders4All")
    fun `should find all orders by query`(
        orders: List<Order>, order: Order, statuses: List<OrderStatusDto>?) = runBlocking<Unit> {
        saveOrder(*orders.shuffled().toTypedArray())

        Wait.waitAssert {
            val result = orderClient.getOrdersAllByStatus(null, null, 1, statuses).awaitFirst()
            assertThat(result.orders.size).isEqualTo(1)
            assertThat(order.hash).isEqualTo(result.orders[0].hash)
        }
    }

    @Test
    fun `should find with SellOrdersByItemAndByStatus`() = runBlocking<Unit> {
        val order1 = createOrderFully()
        val order2 = createOrderFully().copy(make = Asset(Erc721AssetType(AddressFactory.create(), EthUInt256.ONE), EthUInt256.TEN))
        saveOrder(order1, order2)

        Wait.waitAssert {
            val result = orderClient.getSellOrdersByItemAndByStatus(
                order2.make.type.token.toString(),
                order2.make.type.tokenId?.value.toString(),
                order2.maker.toString(),
                null,
                PlatformDto.ALL,
                null, 1, null
            ).awaitFirst()
            assertThat(result.orders.size).isEqualTo(1)
        }
    }

    private fun checkOrderDto(orderDto: OrderDto, order: Order) {
        assertThat(orderDto.hash).isEqualTo(order.hash)
    }

    private suspend fun saveOrder(vararg order: Order) {
        order.forEach { orderRepository.save(it) }
    }
}
