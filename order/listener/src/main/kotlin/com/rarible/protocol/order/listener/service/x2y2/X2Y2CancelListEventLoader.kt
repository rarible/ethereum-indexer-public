package com.rarible.protocol.order.listener.service.x2y2

import com.github.benmanes.caffeine.cache.Caffeine
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
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
            .map { it.order.itemHash }
            .filter { orderStateRepository.getById(it) == null }
            .forEach {
                val cancelState = OrderState(
                    id = it,
                    canceled = true
                )
                logger.x2y2Info("OffChain order cancel $it")
                orderStateRepository.save(cancelState)
                orderUpdateService.update(it)
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
        events.data
            .filter { seenEvents.getIfPresent(it.id) == null }
            .forEach {
                metrics.onOrderReceived(Platform.X2Y2, it.createdAt, "order_event")
                seenEvents.put(it.id, true)
            }
    }
}