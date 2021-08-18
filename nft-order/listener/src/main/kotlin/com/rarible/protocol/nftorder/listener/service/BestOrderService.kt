package com.rarible.protocol.nftorder.listener.service

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.service.OrderService
import com.rarible.protocol.nftorder.listener.evaluator.*
import org.springframework.stereotype.Component

@Component
class BestOrderService(
    private val orderService: OrderService
) {

    suspend fun getBestSellOrder(ownership: Ownership, order: OrderDto): OrderDto? {
        val bestOrderEvaluator = BestOrderEvaluator(
            comparator = BestSellOrderComparator,
            provider = OwnershipBestSellOrderProvider(ownership.id, orderService)
        )
        return bestOrderEvaluator.evaluateBestOrder(ownership.bestSellOrder, order)
    }

    suspend fun getBestSellOrder(item: Item, order: OrderDto): OrderDto? {
        val bestOrderEvaluator = BestOrderEvaluator(
            comparator = BestSellOrderComparator,
            provider = ItemBestSellOrderProvider(item.id, orderService)
        )
        return bestOrderEvaluator.evaluateBestOrder(item.bestSellOrder, order)
    }

    suspend fun getBestBidOrder(item: Item, order: OrderDto): OrderDto? {
        val bestOrderEvaluator = BestOrderEvaluator(
            comparator = BestBidOrderComparator,
            provider = ItemBestBidOrderProvider(item.id, orderService)
        )
        return bestOrderEvaluator.evaluateBestOrder(item.bestBidOrder, order)
    }


}