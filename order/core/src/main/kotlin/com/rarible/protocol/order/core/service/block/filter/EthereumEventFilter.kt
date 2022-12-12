package com.rarible.protocol.order.core.service.block.filter

import com.rarible.ethereum.listener.log.domain.LogEvent

interface EthereumEventFilter {
    fun filter(event: LogEvent): Boolean
}