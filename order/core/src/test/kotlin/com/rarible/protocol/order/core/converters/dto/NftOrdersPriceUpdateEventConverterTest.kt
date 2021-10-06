package com.rarible.protocol.order.core.converters.dto

import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.dto.NftBidOrdersPriceUpdateEventDto
import com.rarible.protocol.dto.NftSellOrdersPriceUpdateEventDto
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.data.createOrderDto
import com.rarible.protocol.order.core.model.ItemId
import com.rarible.protocol.order.core.model.OrderKind
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.AddressFactory
import java.util.stream.Stream

internal class NftOrdersPriceUpdateEventConverterTest {
    private val orderDtoConverter = mockk<OrderDtoConverter>()
    private val nftOrdersPriceUpdateEventConverterTest = NftOrdersPriceUpdateEventConverter(orderDtoConverter)

    companion object {
        @JvmStatic
        fun testArgs(): Stream<Arguments> = Stream.of(
            Arguments.of(
                OrderKind.SELL,
                NftSellOrdersPriceUpdateEventDto::class.java
            ),
            Arguments.of(
                OrderKind.BID,
                NftBidOrdersPriceUpdateEventDto::class.java
            )
        )
    }

    @ParameterizedTest
    @MethodSource("testArgs")
    fun `should convert sell orders to correct event`(
        kind: OrderKind,
        expectedEventType: Class<Any>
    ) = runBlocking<Unit> {
        val order = createOrder()
        val orderDto = createOrderDto()
        val itemId = ItemId(AddressFactory.create(), randomBigInt())

        coEvery { orderDtoConverter.convert(eq(order)) } returns orderDto
        val event = nftOrdersPriceUpdateEventConverterTest.convert(itemId, kind, listOf(order))
        assertThat(event).isInstanceOf(expectedEventType)
        assertThat(event.orders).hasSize(1)
        assertThat(event.orders.single()).isEqualTo(orderDto)
    }
}
