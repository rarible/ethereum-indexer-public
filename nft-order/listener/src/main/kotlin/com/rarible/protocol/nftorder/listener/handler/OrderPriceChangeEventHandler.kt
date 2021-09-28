package com.rarible.protocol.nftorder.listener.handler

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftBidOrdersPriceUpdateEventDto
import com.rarible.protocol.dto.NftOrdersPriceUpdateEventDto
import com.rarible.protocol.dto.NftSellOrdersPriceUpdateEventDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.converter.ShortOrderConverter
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.listener.evaluator.BestBidOrderComparator
import com.rarible.protocol.nftorder.listener.evaluator.BestOrderEvaluator
import com.rarible.protocol.nftorder.listener.evaluator.BestOrderProvider
import com.rarible.protocol.nftorder.listener.evaluator.BestSellOrderComparator
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
            is NftSellOrdersPriceUpdateEventDto -> getBestOrder(bestSellOrderEvaluator, orders)
            is NftBidOrdersPriceUpdateEventDto -> getBestOrder(bestBidOrderEvaluator, orders)
        }
        if (bestOrder != null) {
            orderEventService.updateOrder(bestOrder, forced = true)
        }
    }

    private suspend fun getBestOrder(evaluator: BestOrderEvaluator, orders: List<OrderDto>): OrderDto? {
        return orders.fold(orders.firstOrNull()) { current, updated ->
            val shortCurrent = current?.let { ShortOrderConverter.convert(current) }
            val bestShortOrder = evaluator.evaluateBestOrder(shortCurrent, updated)
            when (bestShortOrder?.hash) {
                current?.hash -> current
                updated.hash -> updated
                else -> null
            }
        }
    }

    private class AbsentBestOrderProvider(itemId: ItemId) : BestOrderProvider<Item> {
        override val entityId = itemId.decimalStringValue
        override val entityType: Class<Item> = Item::class.java
        override suspend fun fetch(): OrderDto? = null
    }
}
