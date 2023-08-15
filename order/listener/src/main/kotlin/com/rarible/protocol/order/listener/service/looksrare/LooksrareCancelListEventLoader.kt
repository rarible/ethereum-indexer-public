package com.rarible.protocol.order.listener.service.looksrare

import com.github.benmanes.caffeine.cache.Caffeine
import com.rarible.looksrare.client.model.v2.LooksrareEvent
import com.rarible.protocol.order.core.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.core.misc.looksrareError
import com.rarible.protocol.order.core.misc.looksrareInfo
import com.rarible.protocol.order.core.misc.orderIntegrationEventMarks
import com.rarible.protocol.order.core.model.LooksrareV2Cursor
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.looksrare.LooksrareService
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class LooksrareCancelListEventLoader(
    private val looksrareService: LooksrareService,
    private val orderStateRepository: OrderStateRepository,
    private val orderUpdateService: OrderUpdateService,
    private val metrics: ForeignOrderMetrics,
    properties: LooksrareLoadProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val seenEvents = Caffeine.newBuilder()
        .maximumSize((properties.loadMaxSize * properties.loadMaxDeep).toLong())
        .build<String, Boolean>()

    suspend fun load(cursor: LooksrareV2Cursor): Result {
        val result = safeGetNextEvents(cursor)
        val saved = AtomicInteger(0)
        result
            .filter {
                val offchain = it.hash == null
                if (!offchain) {
                    logger.info("Skipping cancel event for order ${it.order?.hash} tx: ${it.hash}")
                }
                offchain
            }
            .filter {
                if (it.order == null) return@filter false

                val state = orderStateRepository.getById(it.order!!.hash)
                if (state != null) {
                    logger.looksrareInfo("There is already order state: $state")
                }
                state == null
            }
            .forEach {
                val hash = it.order!!.hash
                val eventTimeMarks = orderIntegrationEventMarks(it.createdAt)
                val cancelState = OrderState(
                    id = hash,
                    canceled = true
                )
                logger.looksrareInfo("OffChain order cancel $hash")
                orderStateRepository.save(cancelState)
                orderUpdateService.update(hash, eventTimeMarks)
                metrics.onOrderEventHandled(Platform.LOOKSRARE, "cancel_offchain")
                saved.incrementAndGet()
            }

        return Result(
            cursor = cursor.next(result),
            saved = saved.get().toLong(),
        )
    }

    private suspend fun safeGetNextEvents(cursor: LooksrareV2Cursor): List<LooksrareEvent> {
        return try {
            val events = looksrareService.getNextCancelListEvents(cursor)
            recordMetrics(events)
            events
        } catch (ex: Throwable) {
            logger.looksrareError("Can't get next events with cursor $cursor", ex)
            throw ex
        }
    }

    private fun recordMetrics(events: List<LooksrareEvent>) {
        fun recordEvent(event: LooksrareEvent) {
            metrics.onOrderReceived(Platform.LOOKSRARE, event.createdAt, "order_event")
        }

        val records = events
            .filter {
                seenEvents.getIfPresent(it.id) == null
            }.map {
                recordEvent(it)
                seenEvents.put(it.id, true)
                it
            }

        if (records.isEmpty()) {
            events.maxByOrNull { it.createdAt }?.let { recordEvent(it) }
        }
    }
}
