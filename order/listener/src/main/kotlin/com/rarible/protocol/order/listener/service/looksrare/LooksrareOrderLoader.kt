package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.looksrare.client.model.v2.LooksrareOrder
import com.rarible.protocol.order.core.misc.orderIntegrationEventMarks
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.protocol.order.listener.misc.looksrareError
import com.rarible.protocol.order.listener.misc.looksrareInfo
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

    suspend fun load(createdAfter: Instant): Result {
        val orders = safeGetNextSellOrders(createdAfter)
        logOrderLoad(orders, createdAfter)
        return coroutineScope {
            val saved = orders
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
                .also { logger.looksrareInfo("Saved ${it.size}") }

            Result(
                latestCreatedAt = orders.maxOfOrNull { it.createdAt },
                saved = saved.size.toLong()
            )
        }
    }

    private suspend fun safeGetNextSellOrders(createdAfter: Instant): List<LooksrareOrder> {
        return try {
            val orders = looksrareOrderService.getNextSellOrders(createdAfter)
            orders.forEach { metrics.onOrderReceived(Platform.LOOKSRARE, it.startTime) }
            orders
        } catch (ex: Throwable) {
            logger.looksrareError("Can't get next orders with createdAfter=${createdAfter.epochSecond}", ex)
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
                append("minCreatedAt: ${orders.minByOrNull { it.createdAt }}, ")
                append("maxCreatedAt: ${orders.maxOfOrNull { it.createdAt }}, ")
                append("newOrders: ${orders.joinToString { it.hash.toString() }}")
            }
        }
        logger.looksrareInfo(logMessage)
    }

    data class Result(
        val latestCreatedAt: Instant?,
        val saved: Long
    )

    private companion object {

        val logger: Logger = LoggerFactory.getLogger(LooksrareOrderLoader::class.java)
    }
}