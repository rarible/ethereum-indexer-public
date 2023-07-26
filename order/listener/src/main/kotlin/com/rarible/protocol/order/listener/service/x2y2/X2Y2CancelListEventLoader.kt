package com.rarible.protocol.order.listener.service.x2y2

import com.github.benmanes.caffeine.cache.Caffeine
import com.rarible.protocol.order.core.misc.orderIntegrationEventMarks
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.x2y2.X2Y2Service
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.protocol.order.listener.misc.x2y2Error
import com.rarible.protocol.order.listener.misc.x2y2Info
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.EVENTS_MAX_LIMIT
import com.rarible.x2y2.client.model.Event
import com.rarible.x2y2.client.model.EventType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class X2Y2CancelListEventLoader(
    private val x2y2Service: X2Y2Service,
    private val orderStateRepository: OrderStateRepository,
    private val orderUpdateService: OrderUpdateService,
    private val metrics: ForeignOrderMetrics
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val seenEvents = Caffeine.newBuilder()
        .maximumSize(EVENTS_MAX_LIMIT.toLong())
        .build<BigInteger, Boolean>()

    suspend fun load(cursor: String?): ApiListResponse<Event> {
        val result = safeGetNextEvents(EventType.CANCEL_LISTING, cursor)
        result.data
            .filter { it.tx == null }
            .filter {
                val state = orderStateRepository.getById(it.order.itemHash)
                if (state != null) {
                    logger.x2y2Info("There is already order state: $state")
                }
                state == null
            }
            .forEach {
                val hash = it.order.itemHash
                val eventTimeMarks = orderIntegrationEventMarks(it.createdAt)
                val cancelState = OrderState(
                    id = hash,
                    canceled = true
                )
                logger.x2y2Info("OffChain order cancel $hash")
                orderStateRepository.save(cancelState)
                orderUpdateService.update(hash, eventTimeMarks)
                metrics.onOrderEventHandled(Platform.X2Y2, "cancel_offchain")
            }

        return result
    }

    private suspend fun safeGetNextEvents(type: EventType, cursor: String?): ApiListResponse<Event> {
        return try {
            val events = x2y2Service.getNextEvents(type, cursor)
            recordMetrics(events)
            events
        } catch (ex: Throwable) {
            logger.x2y2Error("Can't get next events with cursor $cursor", ex)
            throw ex
        }
    }

    private fun recordMetrics(events: ApiListResponse<Event>) {
        fun recordEvent(event: Event) {
            metrics.onOrderReceived(Platform.X2Y2, event.createdAt, "order_event")
        }
        val records = events.data
            .filter {
                seenEvents.getIfPresent(it.id) == null
            }.map {
                recordEvent(it)
                seenEvents.put(it.id, true)
                it
            }

        if (records.isEmpty()) {
            events.data.maxByOrNull { it.createdAt }?.let { recordEvent(it) }
        }
    }
}
