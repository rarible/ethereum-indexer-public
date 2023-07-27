package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary

data class SeaportAdvancedOrder(
    val parameters: SeaportOrderParameters,
    val signature: Binary
)
