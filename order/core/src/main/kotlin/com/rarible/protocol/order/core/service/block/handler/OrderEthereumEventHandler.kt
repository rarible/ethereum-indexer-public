package com.rarible.protocol.order.core.service.block.handler

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderHistory
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
) : AbstractEthereumEventHandler<LogEvent, Word>(properties) {

    override suspend fun handleSingle(event: Word) {
        orderUpdateService.update(event)
    }

    override fun map(events: List<LogEvent>): List<Word> {
        return events
            .asSequence()
            .filter { filter(it)}
            .map { log -> log.data }
            .filterIsInstance<OrderHistory>()
            .map { orderHistory -> orderHistory.hash }
            .distinct()
            .toList()
    }

    private fun filter(event: LogEvent): Boolean {
        return eventFilters.all { it.filter(event) }
    }
}

