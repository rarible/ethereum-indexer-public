package com.rarible.protocol.order.listener.service.event

import com.rarible.ethereum.monitoring.EventCountMetrics
import com.rarible.ethereum.monitoring.EventCountMetrics.EventType
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.listener.service.order.OrderItemService
import org.springframework.stereotype.Component

@Component
class ItemConsumerEventHandler(
    private val orderItemService: OrderItemService,
    properties: OrderIndexerProperties,
    eventCountMetrics: EventCountMetrics
) : InternalEventHandler<NftItemEventDto>(properties, eventCountMetrics) {

    override suspend fun handle(event: NftItemEventDto) = withMetric(EventType.ITEM) {
        when (event) {
            is NftItemUpdateEventDto -> orderItemService.onItemChanged(event)
            else -> {}
        }
    }
}
