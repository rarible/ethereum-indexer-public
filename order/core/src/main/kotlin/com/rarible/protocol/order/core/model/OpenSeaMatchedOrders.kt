package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary

data class OpenSeaMatchedOrders(
    val buyOrder: OpenSeaTransactionOrder,
    val sellOrder: OpenSeaTransactionOrder,
    val origin: Binary?
)
