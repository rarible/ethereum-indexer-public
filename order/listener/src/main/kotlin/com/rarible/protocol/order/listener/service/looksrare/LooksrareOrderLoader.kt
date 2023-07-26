package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.looksrare.client.model.v2.LooksrareOrder
import com.rarible.looksrare.client.model.v2.Status
import com.rarible.protocol.order.core.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.core.misc.looksrareError
import com.rarible.protocol.order.core.misc.looksrareInfo
import com.rarible.protocol.order.core.misc.orderIntegrationEventMarks
import com.rarible.protocol.order.core.model.LooksrareV2Cursor
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.looksrare.LooksrareOrderService
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class LooksrareOrderLoader(
    private val looksrareOrderService: LooksrareOrderService,
    private val looksrareOrderConverter: LooksrareOrderConverter,
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val properties: LooksrareLoadProperties,
    private val metrics: ForeignOrderMetrics
) {

    suspend fun load(cursor: LooksrareV2Cursor): Result {
        val orders = safeGetNextSellOrders(cursor)
        logOrderLoad(orders, cursor.createdAfter)
        return coroutineScope {
            val saved = orders
                .filter { it.status == Status.VALID }
                .chunked(properties.saveBatchSize)
                .map { chunk ->
                    chunk.map {
                        async {
                            if (orderRepository.findById(it.hash) != null) {
                                return@async null
                            }
                            val order = looksrareOrderConverter.convert(it)
                            if (order != null && properties.saveEnabled) {
                                val eventTimeMarks = orderIntegrationEventMarks(order.createdAt)
                                // TODO 2 events will be emitted here - is it fine?
                                orderUpdateService.save(order, eventTimeMarks).also {
                                    orderUpdateService.updateMakeStock(it, null, eventTimeMarks)
                                }
                                metrics.onDownloadedOrderHandled(Platform.LOOKSRARE)
                                logger.looksrareInfo("Saved new order ${it.hash}")
                            }
                            return@async order?.hash
                        }
                    }.awaitAll()
                }
                .flatten()
                .filterNotNull()
                .toList()
                .also { logger.looksrareInfo("Saved ${it.size}") }

            Result(
                cursor = cursor.next(orders),
                saved = saved.size.toLong()
            )
        }
    }

    private suspend fun safeGetNextSellOrders(cursor: LooksrareV2Cursor): List<LooksrareOrder> {
        return try {
            val orders = looksrareOrderService.getNextSellOrders(cursor)
            orders.forEach { metrics.onOrderReceived(Platform.LOOKSRARE, it.startTime) }
            orders.maxOfOrNull { it.createdAt }?.let {
                metrics.onLatestOrderReceived(Platform.LOOKSRARE, it)
            }
            orders
        } catch (ex: Throwable) {
            logger.looksrareError("Can't get next orders with createdAfter=${cursor.createdAfter.epochSecond}", ex)
            throw ex
        }
    }

    private fun logOrderLoad(orders: List<LooksrareOrder>, createdAfter: Instant) {
        val logMessage = if (orders.isEmpty()) {
            "No new orders was fetched"
        } else {
            buildString {
                append("Fetched ${orders.size}, ")
                append("createdAfter=$createdAfter (${createdAfter.epochSecond}), ")
                append("minCreatedAt: ${orders.minOfOrNull { it.createdAt }}, ")
                append("maxCreatedAt: ${orders.maxOfOrNull { it.createdAt }}, ")
                append("newOrders: ${orders.joinToString { it.hash.toString() }}")
            }
        }
        logger.looksrareInfo(logMessage)
    }

    private fun LooksrareV2Cursor.next(orders: List<LooksrareOrder>): LooksrareV2Cursor {
        val max = orders.maxByOrNull { it.createdAt } ?: return this
        val min = orders.minByOrNull { it.createdAt } ?: return this

        // Need to save max seen created order to continue from it after we fetch all old orders
        val savingMaxSeenCreated = maxSeenCreated?.let { maxOf(max.createdAt, it) } ?: max.createdAt

        return if (min.createdAt > createdAfter) {
            logger.looksrareInfo("Still go deep, min createdAfter=${min.createdAt}")
            LooksrareV2Cursor(
                createdAfter = createdAfter,
                nextId = min.id,
                maxSeenCreated = savingMaxSeenCreated
            )
        } else {
            logger.looksrareInfo("Load all, max createdAfter=$savingMaxSeenCreated")
            LooksrareV2Cursor(createdAfter = savingMaxSeenCreated)
        }
    }

    data class Result(
        val cursor: LooksrareV2Cursor?,
        val saved: Long
    )

    private companion object {

        val logger: Logger = LoggerFactory.getLogger(LooksrareOrderLoader::class.java)
    }
}
