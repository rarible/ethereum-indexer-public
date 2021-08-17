package com.rarible.protocol.nftorder.listener.evaluator

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.service.OrderService

class ItemBestBidOrderProvider(
    private val itemId: ItemId,
    private val orderService: OrderService
) : BestOrderProvider<Item, ItemId> {

    override val entityId: ItemId = itemId
    override val entityType: Class<Item> get() = Item::class.java

    override suspend fun fetch(): OrderDto? {
        return orderService.getBestBid(itemId)
    }
}