package com.rarible.protocol.order.listener.service.event

import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.event.NftOrdersPriceUpdateListener
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.order.OrderFilterCriteria.toCriteria
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.listener.misc.makeNftItemId
import com.rarible.protocol.order.listener.misc.takeNftItemId
import com.rarible.protocol.order.listener.service.order.OrderPriceUpdateService
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class OrderUpdateConsumerEventHandler(
    private val orderRepository: OrderRepository,
    private val nftOrdersPriceUpdateListener: NftOrdersPriceUpdateListener,
    private val orderPriceUpdateService: OrderPriceUpdateService
) : ConsumerEventHandler<OrderEventDto> {

    override suspend fun handle(event: OrderEventDto) {
        when (event) {
            is OrderUpdateEventDto -> {
                val at = Instant.now()
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
        val orders = orderRepository.search(itemId.toOrderFilter(kind))
        orders.forEach { orderPriceUpdateService.updateOrderPrice(it.hash, at) }
        nftOrdersPriceUpdateListener.onNftOrders(itemId, kind, orders)
    }

    private fun ItemId.toOrderFilter(kind: OrderKind): Query {
        return when (kind) {
            OrderKind.SELL -> OrderFilterSellByItemDto(
                tokenId = tokenId,
                contract = contract,
                sort = OrderFilterDto.Sort.LAST_UPDATE,
                platform = null,
                maker = null,
                origin = null
            )
            OrderKind.BID -> OrderFilterBidByItemDto(
                tokenId = tokenId,
                contract = contract,
                sort = OrderFilterDto.Sort.LAST_UPDATE,
                platform = null,
                maker = null,
                origin = null
            )
        }.toCriteria(null, null)
    }
}

