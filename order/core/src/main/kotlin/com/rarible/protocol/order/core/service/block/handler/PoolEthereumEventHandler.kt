package com.rarible.protocol.order.core.service.block.handler

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.addIndexerIn
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.pool.listener.PoolOrderEventListener
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PoolEthereumEventHandler(
    private val poolOrderEventListener: PoolOrderEventListener,
    private val orderUpdateService: OrderUpdateService,
    properties: OrderIndexerProperties.PoolEventHandleProperties
) : AbstractEthereumEventHandler<LogRecordEvent, PoolEthereumEventHandler.PoolEvent>(properties) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSingle(event: PoolEvent) {
        val eventTimeMarks = event.events.firstNotNullOfOrNull { it.eventTimeMarks }?.addIndexerIn() ?: orderOffchainEventMarks()
        orderUpdateService.update(event.hash, eventTimeMarks)

        event.events.forEach {
            val ammEventTimeMarks = it.eventTimeMarks.addIndexerIn()
            poolOrderEventListener.onPoolEvent(it.record.asEthereumLogRecord(), ammEventTimeMarks)
        }
    }

    override fun map(events: List<LogRecordEvent>): List<PoolEvent> {
        return events
            .asSequence()
            .filter { log -> log.record.asEthereumLogRecord().data is PoolHistory }
            .groupBy { log -> (log.record.asEthereumLogRecord().data as PoolHistory).hash }
            .map { entity -> PoolEvent(entity.key, entity.value) }
    }

    data class PoolEvent(
        val hash: Word,
        val events: List<LogRecordEvent>
    )
}
