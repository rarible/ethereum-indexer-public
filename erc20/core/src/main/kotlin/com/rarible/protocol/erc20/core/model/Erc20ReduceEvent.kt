package com.rarible.protocol.erc20.core.model

import com.rarible.core.reduce.model.ReduceEvent
import com.rarible.ethereum.listener.log.domain.LogEvent

data class Erc20ReduceEvent(
    val logEvent: LogEvent,
    private val blockNumber: Long
) : ReduceEvent<Long> {
    override val mark = blockNumber
}