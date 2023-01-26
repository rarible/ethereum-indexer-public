package com.rarible.protocol.order.core.service.block.handler

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.dto.blockchainEventMark
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.block.filter.EthereumEventFilter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class OrderEthereumEventHandler(
    private val orderUpdateService: OrderUpdateService,
    @Qualifier("order-event-handler") private val eventFilters: List<EthereumEventFilter>,
    properties: OrderIndexerProperties.OrderEventHandleProperties
) : AbstractEthereumEventHandler<LogRecordEvent, OrderExchangeHistory>(properties) {

    override suspend fun handleSingle(event: OrderExchangeHistory) {
        val sourceEventTimeMark = blockchainEventMark(event.date).source
        orderUpdateService.update(event.hash, sourceEventTimeMark)
    }

    override fun map(events: List<LogRecordEvent>): List<OrderExchangeHistory> {
        return events
            .asSequence()
            .map { record -> record.record.asEthereumLogRecord() }
            .filter { filter(it) }
            .map { log -> log.data }
            .filterIsInstance<OrderExchangeHistory>()
            .distinctBy { it.hash }
            .toList()
    }

    private fun filter(event: ReversedEthereumLogRecord): Boolean {
        return eventFilters.all { it.filter(event) }
    }
}

