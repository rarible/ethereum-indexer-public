package com.rarible.protocol.order.listener.service.event

import com.rarible.core.kafka.RaribleKafkaEventHandler
import com.rarible.ethereum.monitoring.EventCountMetrics
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties

abstract class InternalEventHandler<T>(
    private val properties: OrderIndexerProperties,
    private val eventCountMetrics: EventCountMetrics
) : RaribleKafkaEventHandler<T> {

    protected suspend fun withMetric(type: EventCountMetrics.EventType, delegate: suspend () -> Unit) {
        try {
            eventCountMetrics.eventReceived(EventCountMetrics.Stage.INDEXER, properties.blockchain.value, type)
            delegate()
        } catch (e: Exception) {
            eventCountMetrics.eventReceived(EventCountMetrics.Stage.INDEXER, properties.blockchain.value, type, -1)
            throw e
        }
    }
}
