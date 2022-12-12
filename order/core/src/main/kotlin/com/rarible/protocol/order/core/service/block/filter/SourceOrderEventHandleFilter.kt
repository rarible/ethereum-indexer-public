package com.rarible.protocol.order.core.service.block.filter

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import org.springframework.stereotype.Component

@Component("order-event-handler")
class SourceOrderEventHandleFilter(
    private val properties: OrderIndexerProperties.OrderEventHandleProperties
) : EthereumEventFilter {

    override fun filter(event: LogEvent): Boolean {
        val orderHistory = event.data as? OrderExchangeHistory
        return when (orderHistory?.source) {
            HistorySource.OPEN_SEA -> properties.handleSeaport
            else -> true
        }
    }
}