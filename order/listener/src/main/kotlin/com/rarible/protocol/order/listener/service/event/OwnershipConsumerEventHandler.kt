package com.rarible.protocol.order.listener.service.event

import com.rarible.core.common.asyncWithTraceId
import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.ethereum.monitoring.EventCountMetrics
import com.rarible.ethereum.monitoring.EventCountMetrics.EventType
import com.rarible.ethereum.monitoring.EventCountMetrics.Stage
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.listener.service.order.OrderBalanceService
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OwnershipConsumerEventHandler(
    private val orderBalanceService: OrderBalanceService,
    private val properties: OrderIndexerProperties,
    private val eventCountMetrics: EventCountMetrics
) : RaribleKafkaBatchEventHandler<NftOwnershipEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: List<NftOwnershipEventDto>) {
        val start = System.currentTimeMillis()
        coroutineScope {
            event.groupBy { it.ownershipId }.map { group ->
                asyncWithTraceId(context = NonCancellable) {
                    val ownershipEvents = group.value
                    ownershipEvents.forEach { ownershipEvent ->
                        withMetric(EventType.OWNERSHIP) {
                            logger.info("Got Ownership event: $ownershipEvent")
                            orderBalanceService.handle(ownershipEvent)
                        }
                    }
                }
            }.awaitAll()
        }
        logger.info("Handled group of {} Ownership events ({}ms)", event.size, System.currentTimeMillis() - start)
    }

    protected suspend fun withMetric(type: EventType, delegate: suspend () -> Unit) {
        try {
            eventCountMetrics.eventReceived(Stage.INDEXER_INTERNAL, properties.blockchain.value, type)
            delegate()
        } catch (e: Exception) {
            eventCountMetrics.eventReceived(Stage.INDEXER_INTERNAL, properties.blockchain.value, type, -1)
            throw e
        }
    }
}
