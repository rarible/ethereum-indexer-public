package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.listener.log.domain.LogEvent
import java.time.Instant

data class ItemHistoryEvent(
    val id: String,
    val entityId: String,
    val timestamp: Instant,
    val subEvents: List<ItemHistorySubEvent>
)

sealed class ItemHistorySubEvent {
    abstract val type: ItemHistorySubEventType
}

data class ItemHistoryCreateEvent(
    val logEvent: LogEvent
) : ItemHistorySubEvent() {
    override val type: ItemHistorySubEventType = ItemHistorySubEventType.CREATE
}

enum class ItemHistorySubEventType {
    CREATE
}
