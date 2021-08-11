package com.rarible.protocol.order.core.model

data class CompositeBid(
    val status: BidStatus,
    val version: OrderVersion,
    val order: Order
)

enum class BidStatus {
    ACTIVE,
    FILLED,
    HISTORICAL,
    INACTIVE,
    CANCELLED
}
