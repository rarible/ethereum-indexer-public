package com.rarible.protocol.order.core.model

import java.time.Instant

data class OrderVersionEvent(
    val id: String,
    val entityId: String,
    val timestamp: Instant,
    val subEvents: List<OrderVersionSubEvent>
)

sealed class OrderVersionSubEvent {
    abstract val type: OrderVersionSubEventType
}

data class OrderVersionCreateEvent(
    val orderVersion: OrderVersion
) : OrderVersionSubEvent() {
    override val type: OrderVersionSubEventType = OrderVersionSubEventType.CREATE
}

enum class OrderVersionSubEventType {
    CREATE
}
