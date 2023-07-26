package com.rarible.protocol.order.core.service.block.filter

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord

interface EthereumEventFilter {
    fun filter(event: ReversedEthereumLogRecord): Boolean
}
