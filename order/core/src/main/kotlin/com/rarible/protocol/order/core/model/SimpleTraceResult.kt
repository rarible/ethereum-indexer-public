package com.rarible.protocol.order.core.model

import scalether.domain.Address

data class SimpleTraceResult(
    val type: String?,
    val from: Address,
    val to: Address,
    val input: String,
    val output: String
)
