package com.rarible.protocol.order.core.service.pool.listener

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.misc.toReversedEthereumLogRecord

interface PoolEventListener {
    suspend fun onPoolEvent(event: ReversedEthereumLogRecord)
    suspend fun onPoolEvent(event: LogEvent) {
        return onPoolEvent(event.toReversedEthereumLogRecord())
    }
}