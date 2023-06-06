package com.rarible.protocol.erc20.core.model

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.reduce.model.ReduceEvent

data class Erc20ReduceEvent(
    val logEvent: ReversedEthereumLogRecord,
    private val blockNumber: Long
) : ReduceEvent<Long> {
    override val mark = blockNumber
}