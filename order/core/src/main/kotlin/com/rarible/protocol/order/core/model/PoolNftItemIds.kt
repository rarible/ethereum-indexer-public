package com.rarible.protocol.order.core.model

data class PoolNftItemIds(
    val itemIds: List<ItemId>,
    val continuation: String?
)
