package com.rarible.protocol.order.core.service.block.handler

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.dto.blockchainEventMark
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
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
) : AbstractEthereumEventHandler<LogRecordEvent, PoolEthereumEventHandler.PoolEvent>(properties) {

    override suspend fun handleSingle(event: PoolEvent) {
        val sourceEventTimeMark = event.events.lastOrNull()?.blockTimestamp?.let { blockchainEventMark(it).source }
        orderUpdateService.update(event.hash, sourceEventTimeMark)
        event.events.forEach {
            poolOrderEventListener.onPoolEvent(it)
        }
    }

    override fun map(events: List<LogRecordEvent>): List<PoolEvent> {
        return events
            .map { record -> record.record.asEthereumLogRecord() }
            .filter { log -> log.data is PoolHistory }
            .groupBy { log -> (log.data as PoolHistory).hash }
            .map { entity -> PoolEvent(entity.key, entity.value) }
    }

    data class PoolEvent(
        val hash: Word,
        val events: List<ReversedEthereumLogRecord>
    )
}