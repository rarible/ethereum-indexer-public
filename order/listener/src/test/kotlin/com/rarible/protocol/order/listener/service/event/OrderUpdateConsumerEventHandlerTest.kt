package com.rarible.protocol.order.listener.service.event

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.AddressFactory
import java.util.*
import java.util.stream.Stream

@FlowPreview
@IntegrationTest
internal class OrderUpdateConsumerEventHandlerTest : AbstractIntegrationTest() {
    @Autowired
    protected lateinit var orderDtoConverter: OrderDtoConverter

    @Autowired
    protected lateinit var orderUpdateConsumerEventHandler: OrderUpdateConsumerEventHandler

    companion object {
        @JvmStatic
        fun nftItemOrders(): Stream<Arguments> = run {
            val erc1155 = Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN)
            val erc20 = Erc20AssetType(AddressFactory.create())
            fun createSellOrderVersion() = createOrderVersion().copy(
                make = Asset(erc1155, EthUInt256.TEN),
                take = Asset(erc20, EthUInt256.TEN),
                takeUsd = null,
                makeUsd = null,
                takePriceUsd = null,
                makePriceUsd = null
            )

            fun createBidOrderVersion() = createOrderVersion().copy(
                make = Asset(erc20, EthUInt256.TEN),
                take = Asset(erc1155, EthUInt256.TEN),
                takeUsd = null,
                makeUsd = null,
                takePriceUsd = null,
                makePriceUsd = null
            )
            Stream.of(
                Arguments.arguments(
                    OrderKind.SELL,
                    createSellOrderVersion(),
                    (1..10).map { createSellOrderVersion() }
                ),
                Arguments.arguments(
                    OrderKind.BID,
                    createBidOrderVersion(),
                    (1..10).map { createBidOrderVersion() }
                )
            )
        }
    }

    @ParameterizedTest
    @MethodSource("nftItemOrders")
    fun handleOrderUpdateEvent(
        kind: OrderKind,
        nftOrderVersion: OrderVersion,
        nftOrderVersions: List<OrderVersion>
    ) = runBlocking {
        val nftOrder = save(nftOrderVersion)
        val nftOrders = nftOrderVersions.map { save(it) }
        val event = OrderUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            orderId = nftOrder.hash.toString(),
            order = orderDtoConverter.convert(nftOrder)
        )
        orderUpdateConsumerEventHandler.handle(event)
        nftOrders.map { it.hash }.forEach { hash ->
            orderVersionRepository.findAllByHash(hash).collect { orderVersion ->
                checkOrderVersionPrices(orderVersion, kind)
            }

            val order = checkNotNull(orderRepository.findById(hash))
            checkOrderPrices(order, kind)
        }
    }

    private suspend fun save(orderVersion: OrderVersion): Order {
        return orderUpdateService.save(orderVersion)
    }

    private fun checkOrderPrices(order: Order, kind: OrderKind) {
        when (kind) {
            OrderKind.SELL -> {
                assertThat(order.takeUsd).isNotNull()
                assertThat(order.makePriceUsd).isNotNull()
                assertThat(order.makeUsd).isNull()
                assertThat(order.takePriceUsd).isNull()
            }
            OrderKind.BID -> {
                assertThat(order.makeUsd).isNotNull()
                assertThat(order.takePriceUsd).isNotNull()
                assertThat(order.takeUsd).isNull()
                assertThat(order.makePriceUsd).isNull()
            }
        }
    }

    private fun checkOrderVersionPrices(orderVersion: OrderVersion, kind: OrderKind) {
        when (kind) {
            OrderKind.SELL -> {
                assertThat(orderVersion.takeUsd).isNotNull()
                assertThat(orderVersion.makePriceUsd).isNotNull()
                assertThat(orderVersion.makeUsd).isNull()
                assertThat(orderVersion.takePriceUsd).isNull()
            }
            OrderKind.BID -> {
                assertThat(orderVersion.makeUsd).isNotNull()
                assertThat(orderVersion.takePriceUsd).isNotNull()
                assertThat(orderVersion.takeUsd).isNull()
                assertThat(orderVersion.makePriceUsd).isNull()
            }
        }
    }
}
