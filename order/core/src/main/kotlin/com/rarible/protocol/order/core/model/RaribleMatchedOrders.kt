package com.rarible.protocol.order.core.model

data class RaribleMatchedOrders(
    val left: SimpleOrder,
    val right: SimpleOrder
) {
    data class SimpleOrder(
        val data: OrderData
    )
}
