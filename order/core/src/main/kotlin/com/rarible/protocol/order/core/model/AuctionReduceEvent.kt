package com.rarible.protocol.order.core.model

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.reduce.model.ReduceEvent

data class AuctionReduceEvent(
    val logEvent: ReversedEthereumLogRecord
) : ReduceEvent<Long> {

    override val mark = logEvent.blockNumber ?: 0
}
