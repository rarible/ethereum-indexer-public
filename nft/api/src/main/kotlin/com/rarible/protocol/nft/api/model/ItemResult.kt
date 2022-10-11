package com.rarible.protocol.nft.api.model

import com.rarible.protocol.nft.core.model.ItemId

data class ItemResult(
    val itemId: ItemId,
    val status: ItemStatus,
    val problem: ItemProblemType? = null,
)

enum class ItemStatus {
    VALID,
    INVALID,
    FIXED,
    UNFIXED,
}

fun List<ItemResult>.sorted(): List<ItemResult> {
    return this.sortedWith(compareBy({ it.status }, { it.problem }, { it.itemId.stringValue }))
}
