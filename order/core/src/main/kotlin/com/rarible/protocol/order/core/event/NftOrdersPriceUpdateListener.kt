package com.rarible.protocol.order.core.event

import com.rarible.protocol.order.core.converters.dto.NftOrdersPriceUpdateEventConverter
import com.rarible.protocol.order.core.model.ItemId
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderKind
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import org.springframework.stereotype.Component

@Component
class NftOrdersPriceUpdateListener(
    private val eventPublisher: ProtocolOrderPublisher,
    private val nftOrdersPriceUpdateEventConverter: NftOrdersPriceUpdateEventConverter
) {
    suspend fun onNftOrders(item: ItemId, kind: OrderKind, orders: List<Order>) {
        nftOrdersPriceUpdateEventConverter
            .convert(item, kind, orders)
            .let { eventPublisher.publish(it) }
    }
}

