package com.rarible.protocol.order.core.service.block.handler

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.addIndexerIn
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.block.filter.EthereumEventFilter
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class OrderEthereumEventHandler(
    private val orderUpdateService: OrderUpdateService,
    @Qualifier("order-event-handler") private val eventFilters: List<EthereumEventFilter>,
    properties: OrderIndexerProperties.OrderEventHandleProperties
) : AbstractEthereumEventHandler<LogRecordEvent, OrderEthereumEventHandler.OrderExchangeHistoryWrapper>(properties) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSingle(event: OrderExchangeHistoryWrapper) {
        val eventTimeMarks = event.source.eventTimeMarks.addIndexerIn()
        orderUpdateService.update(event.hash, eventTimeMarks)
    }

    override fun map(events: List<LogRecordEvent>): List<OrderExchangeHistoryWrapper> {
        return events
            .asSequence()
            .mapNotNull { mapOrNull(it) }
            .distinctBy { it.hash }
            .toList()
    }

    private fun mapOrNull(event: LogRecordEvent): OrderExchangeHistoryWrapper? {
        val record = event.record.asEthereumLogRecord()
        if (!eventFilters.all { it.filter(record) }) {
            return null
        }

        val history = record.data as? OrderExchangeHistory ?: return null
        return OrderExchangeHistoryWrapper(
            hash = history.hash,
            event
        )
    }

    data class OrderExchangeHistoryWrapper(
        val hash: Word,
        val source: LogRecordEvent
    )
}
