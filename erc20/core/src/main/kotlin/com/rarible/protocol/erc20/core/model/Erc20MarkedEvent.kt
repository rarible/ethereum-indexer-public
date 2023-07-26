package com.rarible.protocol.erc20.core.model

import com.rarible.core.common.EventTimeMarks

data class Erc20MarkedEvent(
    val event: Erc20Event,
    val eventTimeMarks: EventTimeMarks? = null
)
