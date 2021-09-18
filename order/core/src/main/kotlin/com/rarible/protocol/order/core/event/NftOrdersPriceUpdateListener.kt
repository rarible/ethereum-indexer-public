package com.rarible.protocol.order.core.event

import com.rarible.protocol.order.core.converters.dto.NftOrdersPriceUpdateEventConverter
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import org.springframework.stereotype.Component

@Component
class NftOrdersPriceUpdateListener(
    private val eventPublisher: ProtocolOrderPublisher
) {
    suspend fun onNftOrders(item: ItemId, kind: OrderKind, orders: List<Order>) {
        NftOrdersPriceUpdateEventConverter
            .convert(item, kind, orders)
            .let { eventPublisher.publish(it) }
    }
}

