package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.protocol.order.core.misc.orderIntegrationEventMarks
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.protocol.order.listener.misc.x2y2Error
import com.rarible.protocol.order.listener.misc.x2y2Info
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.Event
import com.rarible.x2y2.client.model.EventType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class X2Y2CancelListEventLoader(
    private val x2y2Service: X2Y2Service,
    private val orderStateRepository: OrderStateRepository,
    private val orderUpdateService: OrderUpdateService,
    private val metrics: ForeignOrderMetrics
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun load(cursor: String?): ApiListResponse<Event> {
        val result = safeGetNextEvents(EventType.CANCEL_LISTING, cursor)
        result.data
            .filter { it.tx == null }
            .filter { orderStateRepository.getById(it.order.itemHash) == null }
            .forEach {
                val hash = it.order.itemHash
                val eventTimeMarks = orderIntegrationEventMarks(it.createdAt)
                val cancelState = OrderState(
                    id = hash,
                    canceled = true
                )
                logger.x2y2Info("OffChain order cancel $hash")
                orderStateRepository.save(cancelState)
                orderUpdateService.update(hash, orderOffchainEventMarks())
                metrics.onOrderEventHandled(Platform.X2Y2, "cancel_offchain")
            }

        return result
    }

    private suspend fun safeGetNextEvents(type: EventType, cursor: String?): ApiListResponse<Event> {
        return try {
            val events = x2y2Service.getNextEvents(type, cursor)
            events.data.forEach { metrics.onOrderReceived(Platform.X2Y2, it.createdAt, "order_event") }
            events
        } catch (ex: Throwable) {
            logger.x2y2Error("Can't get next events with cursor $cursor", ex)
            throw ex
        }
    }
}