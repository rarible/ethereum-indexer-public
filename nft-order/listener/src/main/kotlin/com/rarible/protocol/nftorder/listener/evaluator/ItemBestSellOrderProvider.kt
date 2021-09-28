package com.rarible.protocol.nftorder.listener.evaluator

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.service.OrderService

class ItemBestSellOrderProvider(
    private val itemId: ItemId,
    private val orderService: OrderService
) : BestOrderProvider<Item> {

    override val entityId: String = itemId.decimalStringValue
    override val entityType: Class<Item> get() = Item::class.java

    override suspend fun fetch(): OrderDto? {
        return orderService.getBestSell(itemId)
    }
}