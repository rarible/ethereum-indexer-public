package com.rarible.protocol.nft.core.model

import java.time.Instant

data class ItemEvent(
    val id: String,
    val entityId: String,
    val timestamp: Instant,
    val subEvents: List<ItemSubEvent>
)

sealed class ItemSubEvent {
    abstract val type: SubItemEventType
}

data class UpdateItemEvent(
    val item: Item
) : ItemSubEvent() {
    override val type: SubItemEventType = SubItemEventType.ITEM_UPDATED
}

data class DeleteItemEvent(
    val itemId: String
) : ItemSubEvent() {
    override val type: SubItemEventType = SubItemEventType.ITEM_DELETED
}

enum class SubItemEventType {
    ITEM_UPDATED,
    ITEM_DELETED
}