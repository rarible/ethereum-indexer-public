package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.EventTimeMarks
import com.rarible.core.common.asyncWithTraceId
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.dto.toModel
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.addIndexerIn
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderCancelService
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OrderCollectionService(
    private val orderRepository: OrderRepository,
    private val orderCancelService: OrderCancelService,
    private val featureFlags: OrderIndexerProperties.FeatureFlags,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun onCollectionChanged(event: NftCollectionUpdateEventDto) {
        val eventTimeMarks = event.eventTimeMarks?.toModel()?.addIndexerIn() ?: run {
            logger.warn("EventTimeMarks not found in NftCollectionUpdateEventDto")
            orderOffchainEventMarks()
        }
        onCollectionChanged(event.collection, eventTimeMarks)
    }

    suspend fun onCollectionChanged(
        collection: NftCollectionDto,
        eventTimeMarks: EventTimeMarks
    ) {
        if (featureFlags.ignoreTokenPause) return
        if (collection.flags?.paused == false) return
        val start = System.currentTimeMillis()
        val toCancel = orderRepository.findNonTerminateOrdersByToken(collection.id)
            .take(ORDER_TO_CANCEL_MAX_COUNT).map { it.hash }.toList()
        // Should never happen since there are not a lot of such items/orders, but let's monitor it
        if (toCancel.size == ORDER_TO_CANCEL_MAX_COUNT) {
            logger.warn("WARNING! Too many active Orders found for Collection {} which was paused", collection.id)
        }
        if (toCancel.isEmpty()) {
            return
        }
        coroutineScope {
            toCancel.chunked(ORDER_TO_CANCEL_CHUNK_SIZE).forEach { chunk ->
                chunk.map { hash ->
                    asyncWithTraceId(context = NonCancellable) {
                        logger.info(
                            "Cancelled Order {} for Collection {} which was paused",
                            hash.prefixed(),
                            collection.id
                        )
                        orderCancelService.cancelOrder(hash, eventTimeMarks)
                    }
                }.awaitAll()
            }
        }
        logger.info(
            "Cancelled {} active Orders for Collection {} which was paused ({}ms)",
            toCancel.size,
            collection.id,
            System.currentTimeMillis() - start
        )
    }

    companion object {
        const val ORDER_TO_CANCEL_MAX_COUNT = 5000
        const val ORDER_TO_CANCEL_CHUNK_SIZE = 100
    }
}
