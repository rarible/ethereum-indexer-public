package com.rarible.protocol.order.listener.service.event

import com.rarible.ethereum.monitoring.EventCountMetrics
import com.rarible.ethereum.monitoring.EventCountMetrics.EventType
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.listener.service.order.OrderCollectionService
import org.springframework.stereotype.Component

@Component
class CollectionConsumerEventHandler(
    private val orderCollectionService: OrderCollectionService,
    properties: OrderIndexerProperties,
    eventCountMetrics: EventCountMetrics
) : InternalEventHandler<NftCollectionEventDto>(properties, eventCountMetrics) {

    override suspend fun handle(event: NftCollectionEventDto) = withMetric(EventType.COLLECTION) {
        when (event) {
            is NftCollectionUpdateEventDto -> orderCollectionService.onCollectionChanged(event)
            else -> {}
        }
    }
}
