package com.rarible.protocol.order.core.model

data class ReduceResult(
    val order: Order,
    val eventId: String
) {
    fun withOrder(order: Order): ReduceResult {
        return copy(order = order)
    }
}
