package com.rarible.protocol.order.listener.service.event

import com.rarible.ethereum.monitoring.EventCountMetrics
import com.rarible.ethereum.monitoring.EventCountMetrics.EventType
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.listener.service.order.OrderBalanceService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Erc20BalanceConsumerEventHandler(
    private val orderBalanceService: OrderBalanceService,
    properties: OrderIndexerProperties,
    eventCountMetrics: EventCountMetrics
) : InternalEventHandler<Erc20BalanceEventDto>(properties, eventCountMetrics) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: Erc20BalanceEventDto) = withMetric(EventType.ERC20) {
        logger.info("Got erc20 event: $event")
        orderBalanceService.handle(event)
    }
}
