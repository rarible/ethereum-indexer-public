package com.rarible.protocol.order.listener.service.event

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.event.NftOrdersPriceUpdateListener
import com.rarible.protocol.order.core.misc.MAX_SIZE
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.service.OrderRepositoryService
import com.rarible.protocol.order.listener.misc.makeNftItemId
import com.rarible.protocol.order.listener.misc.takeNftItemId
import com.rarible.protocol.order.listener.service.order.OrderPriceUpdateService
import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class OrderUpdateConsumerEventHandler(
    private val orderRepositoryService: OrderRepositoryService,
    private val nftOrdersPriceUpdateListener: NftOrdersPriceUpdateListener,
    private val orderPriceUpdateService: OrderPriceUpdateService
) : ConsumerEventHandler<OrderEventDto> {

    override suspend fun handle(event: OrderEventDto) {
        when (event) {
            is OrderUpdateEventDto -> {
                val at = nowMillis()
                val order = event.order
                orderPriceUpdateService.updateOrderVersionPrice(order.hash, at)

                val makeNftItemId = order.makeNftItemId
                if (makeNftItemId != null) {
                    updateItemOrders(makeNftItemId, OrderKind.SELL, at)
                }

                val takeNftItemId = order.takeNftItemId
                if (takeNftItemId != null) {
                    updateItemOrders(takeNftItemId, OrderKind.BID, at)
                }
            }
        }
    }

    private suspend fun updateItemOrders(itemId: ItemId, kind: OrderKind, at: Instant) {
        orderRepositoryService.search(itemId.toOrderFilter(kind), MAX_SIZE).collect { orders ->
            orders.forEach { orderPriceUpdateService.updateOrderPrice(it.hash, at) }
            nftOrdersPriceUpdateListener.onNftOrders(itemId, kind, orders)
        }
    }

    private fun ItemId.toOrderFilter(kind: OrderKind): OrderFilterDto {
        return when (kind) {
            OrderKind.SELL -> OrderFilterSellByItemDto(
                tokenId = tokenId,
                contract = contract,
                sort = OrderFilterDto.Sort.LAST_UPDATE_DESC,
                platform = null,
                maker = null,
                origin = null
            )
            OrderKind.BID -> OrderFilterBidByItemDto(
                tokenId = tokenId,
                contract = contract,
                sort = OrderFilterDto.Sort.LAST_UPDATE_DESC,
                platform = null,
                maker = null,
                origin = null
            )
        }
    }
}

