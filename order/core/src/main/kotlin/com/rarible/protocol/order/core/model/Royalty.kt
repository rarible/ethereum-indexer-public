package com.rarible.protocol.order.core.model

import scalether.domain.Address

data class Royalty(
    val recipient: Address,
    val value: Long
)
