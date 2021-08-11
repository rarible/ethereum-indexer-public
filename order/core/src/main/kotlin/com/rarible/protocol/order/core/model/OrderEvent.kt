package com.rarible.protocol.order.core.model

import java.time.Instant

data class OrderEvent(
    val id: String,
    val entityId: String,
    val timestamp: Instant,
    val subEvents: List<OrderSubEvent>
)

sealed class OrderSubEvent {
    abstract val type: OrderSubEventType
}

data class OrderUpdateEvent(
    val order: Order
) : OrderSubEvent() {
    override val type: OrderSubEventType = OrderSubEventType.UPDATE
}

enum class OrderSubEventType {
    UPDATE
}
