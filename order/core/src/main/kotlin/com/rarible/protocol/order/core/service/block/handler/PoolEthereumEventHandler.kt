package com.rarible.protocol.order.core.service.block.handler

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.service.pool.listener.PoolOrderEventListener
import org.springframework.stereotype.Component

@Component
class PoolEthereumEventHandler(
    private val poolOrderEventListener: PoolOrderEventListener,
    properties: OrderIndexerProperties.PoolEventHandleProperties
) : AbstractEthereumEventHandler<LogEvent, LogEvent>(properties) {
    override suspend fun handleSingle(event: LogEvent) {
        poolOrderEventListener.onPoolEvent(event)
    }

    override fun map(events: List<LogEvent>): List<LogEvent> {
        return events.filter { log -> log.data is PoolHistory }
    }
}