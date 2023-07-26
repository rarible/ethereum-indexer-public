package com.rarible.protocol.order.core.model

import java.time.Instant

data class Continuation(
    val afterDate: Instant,
    val afterId: String
)
