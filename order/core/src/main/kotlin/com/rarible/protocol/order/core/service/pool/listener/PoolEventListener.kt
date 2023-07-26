package com.rarible.protocol.order.core.service.pool.listener

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.common.EventTimeMarks

interface PoolEventListener {

    suspend fun onPoolEvent(event: ReversedEthereumLogRecord, eventTimeMarks: EventTimeMarks)
}
