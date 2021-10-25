package com.rarible.protocol.order.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderSortDto
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.api.data.createErc721BidOrderVersion
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
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.math.BigInteger
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

        @JvmStatic
        fun orders4All(): Stream<Arguments> = run {
            val order: () -> Order = { createOrderFully().copy(makeStock = EthUInt256.ZERO) }
            val inactiveOrder = order()
            val activeOrder = order().copy(makeStock = EthUInt256.ONE).copy()
            val filledOrder = order().copy(fill = EthUInt256.TEN, take = Asset(EthAssetType, EthUInt256.TEN)).copy()
            val canceledOrder = order().copy(cancelled = true).copy()
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
                null, 1, null, null
            ).awaitFirst()
            assertThat(result.orders.size).isEqualTo(1)
        }
    }

    @Test
    fun `should find sell-orders by currency and sort asc`() = runBlocking<Unit> {
        val makeAddress = AddressFactory.create()
        val currencyToken = AddressFactory.create()
        val order1 = createOrderFully().copy(
            make = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            take = Asset(Erc20AssetType(currencyToken), EthUInt256.of(1)),
            makePrice = BigDecimal.valueOf(1L)
        )
        val order2 = createOrderFully().copy(
            maker = order1.maker,
            make = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            take = Asset(Erc20AssetType(currencyToken), EthUInt256.of(2)),
            makePrice = BigDecimal.valueOf(2L)
        )
        val order3 = createOrderFully().copy(
            maker = order1.maker,
            make = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(1)),
            makePrice = BigDecimal.valueOf(1L)
        )
        saveOrder(order3, order2, order1)

        val result = orderClient.getSellOrdersByItemAndByStatus(
            order1.make.type.token.toString(),
            order1.make.type.tokenId?.value.toString(),
            order1.maker.toString(),
            null,
            PlatformDto.ALL,
            null, 1, null, currencyToken.hex()
        ).awaitFirst()
        assertThat(result.orders.size).isEqualTo(1)
        assertThat(result.orders[0].take.value).isEqualTo(1)

        val result2 = orderClient.getSellOrdersByItemAndByStatus(
            order1.make.type.token.toString(),
            order1.make.type.tokenId?.value.toString(),
            order1.maker.toString(),
            null,
            PlatformDto.ALL,
            result.continuation, 2, null, currencyToken.hex()
        ).awaitFirst()
        assertThat(result2.orders.size).isEqualTo(1)
        assertThat(result2.orders[0].take.value).isEqualTo(2)
        assertThat(result2.continuation).isNull()
    }

    @Test
    fun `should find bid-orders by currency and sort desc`() = runBlocking<Unit> {
        val makeAddress = AddressFactory.create()
        val currencyToken = AddressFactory.create()
        val order1V = createErc721BidOrderVersion().copy(
            make = Asset(Erc20AssetType(currencyToken), EthUInt256.of(1)),
            take = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            takePrice = BigDecimal.valueOf(1L)
        )
        val order2V = createErc721BidOrderVersion().copy(
            maker = order1V.maker,
            make = Asset(Erc20AssetType(currencyToken), EthUInt256.of(2)),
            take = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            takePrice = BigDecimal.valueOf(2L)
        )
        saveOrderVersions(order1V, order2V)

        val result = orderClient.getOrderBidsByItemAndByStatus(
            order1V.take.type.token.toString(),
            order1V.take.type.tokenId?.value.toString(),
            OrderStatusDto.values().toList(),
            order1V.maker.toString(),
            null,
            PlatformDto.ALL,
            null, 1, currencyToken.hex(), null, null
        ).awaitFirst()
        assertThat(result.orders.size).isEqualTo(1)
        assertThat(result.orders[0].make.value).isEqualTo(2)

        val result2 = orderClient.getOrderBidsByItemAndByStatus(
            order1V.take.type.token.toString(),
            order1V.take.type.tokenId?.value.toString(),
            OrderStatusDto.values().toList(),
            order1V.maker.toString(),
            null,
            PlatformDto.ALL,
            result.continuation, 2, currencyToken.hex(), null, null
        ).awaitFirst()
        assertThat(result2.orders.size).isEqualTo(1)
        assertThat(result2.orders[0].make.value).isEqualTo(1)
        assertThat(result2.continuation).isNull()
    }

    @Test
    fun `should find sell-orders by ETH currency`() = runBlocking<Unit> {
        val makeAddress = AddressFactory.create()
        val currencyToken = Address.ZERO()
        val order1 = createOrderFully().copy(
            make = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            take = Asset(EthAssetType, EthUInt256.of(123)),
            makePrice = BigDecimal.valueOf(123L)
        )
        saveOrder(order1)

        val result = orderClient.getSellOrdersByItemAndByStatus(
            order1.make.type.token.toString(),
            order1.make.type.tokenId?.value.toString(),
            order1.maker.toString(),
            null,
            PlatformDto.ALL,
            null, 1, null, currencyToken.hex()
        ).awaitFirst()
        assertThat(result.orders.size).isEqualTo(1)
        assertThat(result.orders[0].take.value).isEqualTo(123)
    }

    @Test
    fun `should find bid-order by ETH currency`() = runBlocking<Unit> {
        val makeAddress = AddressFactory.create()
        val currencyToken = Address.ZERO()
        val order1V = createErc721BidOrderVersion().copy(
            make = Asset(EthAssetType, EthUInt256.of(123)),
            take = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            takePrice = BigDecimal.valueOf(123L)
        )
        saveOrderVersions(order1V)

        val result = orderClient.getOrderBidsByItemAndByStatus(
            order1V.take.type.token.toString(),
            order1V.take.type.tokenId?.value.toString(),
            OrderStatusDto.values().toList(),
            order1V.maker.toString(),
            null,
            PlatformDto.ALL,
            null, 1, currencyToken.hex(), null, null
        ).awaitFirst()
        assertThat(result.orders.size).isEqualTo(1)
        assertThat(result.orders[0].make.value).isEqualTo(123)
    }

    @Test
    fun `should find bid-order by currency`() = runBlocking<Unit> {
        val makeAddress = AddressFactory.create()
        val currencyToken = AddressFactory.create()
        val order1V = createErc721BidOrderVersion().copy(
            make = Asset(Erc20AssetType(currencyToken), EthUInt256.of(123)),
            take = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            takePrice = BigDecimal.valueOf(123L)
        )
        saveOrderVersions(order1V)
        val order1VEth = createErc721BidOrderVersion().copy(
            maker = order1V.maker,
            make = Asset(EthAssetType, EthUInt256.of(123)),
            take = order1V.take,
            takePrice = BigDecimal.valueOf(123L)
        )
        saveOrderVersions(order1VEth)

        val result = orderClient.getOrderBidsByItemAndByStatus(
            order1V.take.type.token.toString(),
            order1V.take.type.tokenId?.value.toString(),
            OrderStatusDto.values().toList(),
            order1V.maker.toString(),
            null,
            PlatformDto.ALL,
            null, 2, currencyToken.hex(), null, null
        ).awaitFirst()
        assertThat(result.orders.size).isEqualTo(1)
        assertThat(result.orders[0].make.value).isEqualTo(123)
    }

    private fun checkOrderDto(orderDto: OrderDto, order: Order) {
        assertThat(orderDto.hash).isEqualTo(order.hash)
    }

    private suspend fun saveOrder(vararg order: Order) {
        order.forEach { orderRepository.save(it) }
    }

    private suspend fun saveOrderVersions(vararg order: OrderVersion) {
        order.forEach { orderUpdateService.save(it) }
    }
}
