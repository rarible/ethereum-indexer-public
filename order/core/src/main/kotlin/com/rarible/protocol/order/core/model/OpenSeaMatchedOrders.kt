package com.rarible.protocol.order.core.model

data class OpenSeaMatchedOrders(
    val buyOrder: OpenSeaTransactionOrder,
    val sellOrder: OpenSeaTransactionOrder,
    val externalOrderExecutedOnRarible: Boolean
)

