package com.rarible.protocol.nftorder.listener.service

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.model.ShortOrder
import com.rarible.protocol.nftorder.core.service.OrderService
import com.rarible.protocol.nftorder.listener.evaluator.*
import org.springframework.stereotype.Component

@Component
class BestOrderService(
    private val orderService: OrderService
) {

    // TODO we can return here Full Order if it was fetched - thats allow us to avoid one more query to indexer
    // for update events in ownership/item
    suspend fun getBestSellOrder(ownership: Ownership, order: OrderDto): ShortOrder? {
        val bestOrderEvaluator = BestOrderEvaluator(
            comparator = BestSellOrderComparator,
            provider = OwnershipBestSellOrderProvider(ownership.id, orderService)
        )
        return bestOrderEvaluator.evaluateBestOrder(ownership.bestSellOrder, order)
    }

    suspend fun getBestSellOrder(item: Item, order: OrderDto): ShortOrder? {
        val bestOrderEvaluator = BestOrderEvaluator(
            comparator = BestSellOrderComparator,
            provider = ItemBestSellOrderProvider(item.id, orderService)
        )
        return bestOrderEvaluator.evaluateBestOrder(item.bestSellOrder, order)
    }

    suspend fun getBestBidOrder(item: Item, order: OrderDto): ShortOrder? {
        val bestOrderEvaluator = BestOrderEvaluator(
            comparator = BestBidOrderComparator,
            provider = ItemBestBidOrderProvider(item.id, orderService)
        )
        return bestOrderEvaluator.evaluateBestOrder(item.bestBidOrder, order)
    }

}