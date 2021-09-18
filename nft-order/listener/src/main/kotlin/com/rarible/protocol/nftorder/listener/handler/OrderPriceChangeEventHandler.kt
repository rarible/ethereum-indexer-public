package com.rarible.protocol.nftorder.listener.handler

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftBidOrdersPriceUpdateEventDto
import com.rarible.protocol.dto.NftOrdersPriceUpdateEventDto
import com.rarible.protocol.dto.NftSellOrdersPriceUpdateEventDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.listener.evaluator.*
import com.rarible.protocol.nftorder.listener.service.OrderEventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OrderPriceChangeEventHandler(
    private val orderEventService: OrderEventService
) : AbstractEventHandler<NftOrdersPriceUpdateEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftOrdersPriceUpdateEventDto) {
        val orders = event.orders
        val itemId = ItemId(event.contract, EthUInt256.Companion.of(event.tokenId))

        val bestSellOrderEvaluator = BestOrderEvaluator(
            comparator = BestSellOrderComparator,
            provider = AbsentBestOrderProvider(itemId)
        )
        val bestBidOrderEvaluator = BestOrderEvaluator(
            comparator = BestBidOrderComparator,
            provider = AbsentBestOrderProvider(itemId)
        )
        logger.debug(
            "Received Order change price event: type=${event::class.java.simpleName}, orders=${orders.size}"
        )
        val bestOrder = when (event) {
            is NftSellOrdersPriceUpdateEventDto -> {
                orders.fold(orders.firstOrNull()) { current, updated ->
                    bestSellOrderEvaluator.evaluateBestOrder(current, updated)
                }
            }
            is NftBidOrdersPriceUpdateEventDto -> {
                orders.fold(orders.firstOrNull()) { current, updated ->
                    bestBidOrderEvaluator.evaluateBestOrder(current, updated)
                }
            }
        }
        if (bestOrder != null) {
            orderEventService.updateOrder(bestOrder)
        }
    }

    private class AbsentBestOrderProvider(itemId: ItemId) : BestOrderProvider<Item, ItemId> {
        override val entityId = itemId
        override val entityType: Class<Item> = Item::class.java

        override suspend fun fetch(): OrderDto? {
            return null
        }
    }
}
