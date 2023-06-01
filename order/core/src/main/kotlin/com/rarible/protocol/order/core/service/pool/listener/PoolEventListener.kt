package com.rarible.protocol.order.core.service.pool.listener

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.common.EventTimeMarks
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.misc.toReversedEthereumLogRecord

interface PoolEventListener {

    suspend fun onPoolEvent(event: ReversedEthereumLogRecord, eventTimeMarks: EventTimeMarks)
    suspend fun onPoolEvent(event: LogEvent, eventTimeMarks: EventTimeMarks) {
        return onPoolEvent(event.toReversedEthereumLogRecord(), eventTimeMarks)
    }
}