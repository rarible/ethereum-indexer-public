package com.rarible.protocol.nft.core.model

data class ItemsSearchResult(
    val items: List<Item>,
    val meta: Map<ItemId, ItemMeta>
)