package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent

interface Erc20EventListener {
    suspend fun onEntityEvents(events: List<LogRecordEvent>)
}
