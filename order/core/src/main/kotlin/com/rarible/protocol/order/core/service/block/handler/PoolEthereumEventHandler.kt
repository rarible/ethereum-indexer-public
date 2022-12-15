package com.rarible.protocol.order.core.service.block.handler

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.pool.listener.PoolOrderEventListener
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component

@Component
class PoolEthereumEventHandler(
    private val poolOrderEventListener: PoolOrderEventListener,
    private val orderUpdateService: OrderUpdateService,
    properties: OrderIndexerProperties.PoolEventHandleProperties
) : AbstractEthereumEventHandler<LogEvent, PoolEthereumEventHandler.PoolEvent>(properties) {
    override suspend fun handleSingle(event: PoolEvent) {
        orderUpdateService.update(event.hash)
        event.events.forEach {
            poolOrderEventListener.onPoolEvent(it)
        }
    }

    override fun map(events: List<LogEvent>): List<PoolEvent> {
        return events
            .filter { log -> log.data is PoolHistory }
            .groupBy { log -> (log.data as PoolHistory).hash }
            .map { entity -> PoolEvent(entity.key, entity.value) }
    }

    data class PoolEvent(
        val hash: Word,
        val events: List<LogEvent>
    )
}