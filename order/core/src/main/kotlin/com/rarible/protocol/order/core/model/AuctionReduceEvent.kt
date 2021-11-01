package com.rarible.protocol.order.core.model

import com.rarible.core.reduce.model.ReduceEvent
import com.rarible.ethereum.listener.log.domain.LogEvent

data class AuctionReduceEvent(
    val logEvent: LogEvent
) : ReduceEvent<Long> {
    override val mark = logEvent.blockNumber ?: 0
}
