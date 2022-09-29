package com.rarible.protocol.order.core.service.pool.listener

import com.rarible.ethereum.listener.log.domain.LogEvent

interface PoolEventListener {
    suspend fun onPoolEvent(event: LogEvent)
}