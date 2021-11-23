package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import scalether.domain.Address

data class SimpleTraceResult(
    val from: Address,
    val to: Address?,
    val input: Binary
)
