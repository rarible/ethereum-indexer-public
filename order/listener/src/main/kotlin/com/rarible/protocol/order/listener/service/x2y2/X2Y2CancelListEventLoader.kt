package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.misc.x2y2Error
import com.rarible.protocol.order.listener.misc.x2y2Info
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.Event
import com.rarible.x2y2.client.model.EventType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class X2Y2CancelListEventLoader(
    private val x2y2Service: X2Y2Service,
    private val orderStateRepository: OrderStateRepository,
    private val orderUpdateService: OrderUpdateService,
    private val x2y2OffChainOrderCancelCounter: RegisteredCounter
) {
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
                x2y2OffChainOrderCancelCounter.increment()
            }

        return result
    }

    private suspend fun safeGetNextEvents(type: EventType, cursor: String?): ApiListResponse<Event> {
        return try {
            x2y2Service.getNextEvents(type, cursor)
        } catch (ex: Throwable) {
            logger.x2y2Error("Can't get next events with cursor $cursor", ex)
            throw ex
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(X2Y2CancelListEventLoader::class.java)
    }
}