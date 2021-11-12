package com.rarible.protocol.order.listener.service.event

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.event.NftOrdersPriceUpdateListener
import com.rarible.protocol.order.core.misc.MAX_SIZE
import com.rarible.protocol.order.core.model.ItemId
import com.rarible.protocol.order.core.model.OrderKind
import com.rarible.protocol.order.core.service.OrderRepositoryService
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

                val makeItemId = order.make.assetType.getItemId()
                if (makeItemId != null) {
                    updateItemOrders(makeItemId, OrderKind.SELL, at)
                }

                val takeItemId = order.take.assetType.getItemId()
                if (takeItemId != null) {
                    updateItemOrders(takeItemId, OrderKind.BID, at)
                }
            }
        }
    }

    private suspend fun updateItemOrders(itemId: ItemId, kind: OrderKind, at: Instant) {
        val orderFilter = when (kind) {
            OrderKind.SELL -> OrderFilterSellByItemDto(
                tokenId = itemId.tokenId,
                contract = itemId.contract,
                sort = OrderFilterDto.Sort.LAST_UPDATE_DESC,
                platforms = emptyList(),
                maker = null,
                origin = null
            )
            OrderKind.BID -> OrderFilterBidByItemDto(
                tokenId = itemId.tokenId,
                contract = itemId.contract,
                sort = OrderFilterDto.Sort.LAST_UPDATE_DESC,
                platforms = emptyList(),
                maker = null,
                origin = null
            )
        }
        orderRepositoryService.search(orderFilter, MAX_SIZE).collect { orders ->
            orders.forEach { orderPriceUpdateService.updateOrderPrice(it.hash, at) }
            nftOrdersPriceUpdateListener.onNftOrders(itemId, kind, orders)
        }
    }

    private fun AssetTypeDto.getItemId(): ItemId? = when (this) {
        is CryptoPunksAssetTypeDto -> ItemId(contract, punkId.toBigInteger())
        is Erc1155AssetTypeDto -> ItemId(contract, tokenId)
        is Erc1155LazyAssetTypeDto -> ItemId(contract, tokenId)
        is Erc721AssetTypeDto -> ItemId(contract, tokenId)
        is Erc721LazyAssetTypeDto -> ItemId(contract, tokenId)
        is Erc20AssetTypeDto, is EthAssetTypeDto, is GenerativeArtAssetTypeDto, is CollectionAssetTypeDto -> null
    }
}

