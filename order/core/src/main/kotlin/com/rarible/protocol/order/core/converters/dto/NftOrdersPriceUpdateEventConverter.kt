package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.NftOrdersPriceUpdateEventDto
import com.rarible.protocol.dto.NftSellOrdersPriceUpdateEventDto
import com.rarible.protocol.order.core.model.ItemId
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderKind
import java.util.*

object NftOrdersPriceUpdateEventConverter {
    fun convert(item: ItemId, kind: OrderKind, orders: List<Order>): NftOrdersPriceUpdateEventDto {
        val eventId = UUID.randomUUID().toString()

        return when (kind) {
            OrderKind.SELL -> NftSellOrdersPriceUpdateEventDto(
                contract = item.contract,
                tokenId = item.tokenId,
                orders = orders.map { OrderDtoConverter.convert(it) },
                eventId = eventId
            )
            OrderKind.BID -> NftSellOrdersPriceUpdateEventDto(
                contract = item.contract,
                tokenId = item.tokenId,
                orders = orders.map { OrderDtoConverter.convert(it) },
                eventId = eventId
            )
        }
    }
}
