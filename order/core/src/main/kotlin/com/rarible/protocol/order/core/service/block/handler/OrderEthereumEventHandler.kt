package com.rarible.protocol.order.core.service.block.handler

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.block.filter.EthereumEventFilter
import io.daonomic.rpc.domain.Word
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class OrderEthereumEventHandler(
    private val orderUpdateService: OrderUpdateService,
    @Qualifier("order-event-handler") private val eventFilters: List<EthereumEventFilter>,
    properties: OrderIndexerProperties.OrderEventHandleProperties
) : AbstractEthereumEventHandler<LogRecordEvent, Word>(properties) {

    override suspend fun handleSingle(event: Word) {
        orderUpdateService.update(event)
    }

    override fun map(events: List<LogRecordEvent>): List<Word> {
        return events
            .asSequence()
            .map { record -> record.record.asEthereumLogRecord() }
            .filter { filter(it)}
            .map { log -> log.data }
            .filterIsInstance<OrderExchangeHistory>()
            .map { orderHistory -> orderHistory.hash }
            .distinct()
            .toList()
    }

    private fun filter(event: ReversedEthereumLogRecord): Boolean {
        return eventFilters.all { it.filter(event) }
    }
}

