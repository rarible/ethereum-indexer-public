package com.rarible.protocol.order.core.model

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.reduce.model.ReduceEvent
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.misc.toReversedEthereumLogRecord

data class AuctionReduceEvent(
    val logEvent: ReversedEthereumLogRecord
) : ReduceEvent<Long> {
    constructor(logEvent: LogEvent): this(logEvent.toReversedEthereumLogRecord())

    override val mark = logEvent.blockNumber ?: 0
}
