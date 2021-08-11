package com.rarible.protocol.order.core.model

import com.rarible.ethereum.listener.log.domain.LogEvent
import java.time.Instant

data class OrderExchangeHistoryEvent(
    val id: String,
    val entityId: String,
    val timestamp: Instant,
    val subEvents: List<OrderExchangeHistorySubEvent>
)

sealed class OrderExchangeHistorySubEvent {
    abstract val type: OrderExchangeHistorySubEventType
}

data class OrderExchangeHistoryCreateEvent(
    val logEvent: LogEvent
) : OrderExchangeHistorySubEvent() {
    override val type: OrderExchangeHistorySubEventType = OrderExchangeHistorySubEventType.CREATE
}

enum class OrderExchangeHistorySubEventType {
    CREATE
}
