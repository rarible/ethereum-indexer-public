package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.logging.Logger
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.telemetry.metrics.RegisteredGauge
import com.rarible.looksrare.client.model.v1.LooksrareOrder
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.listener.misc.looksrareError
import com.rarible.protocol.order.listener.misc.looksrareInfo
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class LooksrareOrderLoader(
    private val looksrareOrderService: LooksrareOrderService,
    private val looksrareOrderConverter: LooksrareOrderConverter,
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val properties: LooksrareLoadProperties,
    private val looksrareSaveCounter: RegisteredCounter,
    private val looksrareOrderDelayGauge: RegisteredGauge<Long>
) {
    suspend fun load(
        listedAfter: Instant,
        listedBefore: Instant
    ): List<Word> {
        val orders = safeGetNextSellOrders(listedAfter, listedBefore)
        logOrderLoad(orders, listedAfter, listedBefore)
        return coroutineScope {
            orders
                .chunked(properties.saveBatchSize)
                .map { chunk ->
                    chunk.map {
                        async {
                            if (orderRepository.findById(it.hash) != null) {
                                return@async null
                            }
                            val order = looksrareOrderConverter.convert(it)
                            if (order != null && properties.saveEnabled) {
                                orderUpdateService.save(order)
                                looksrareSaveCounter.increment()
                                logger.looksrareInfo("Saved new order ${it.hash}")
                            }
                            return@async order?.hash
                        }
                    }.awaitAll()
                }
                .flatten()
                .filterNotNull()
                .also { logger.looksrareInfo("Saved ${it.size}") }
        }
    }

    private suspend fun safeGetNextSellOrders(listedAfter: Instant, listedBefore: Instant): List<LooksrareOrder> {
        return try {
            looksrareOrderService.getNextSellOrders(listedAfter, listedBefore).also { orders ->
                orders.maxOfOrNull { it.startTime }?.let {
                    looksrareOrderDelayGauge.set(Instant.now().epochSecond - it.epochSecond)
                }
            }
        } catch (ex: Throwable) {
            logger.looksrareError("Can't get next orders with listedAfter=${listedAfter.epochSecond}, listedBefore=${listedBefore.epochSecond}", ex)
            throw ex
        }
    }

    private fun logOrderLoad(orders: List<LooksrareOrder>, listedAfter: Instant, listedBefore: Instant) {
        val logMessage = if (orders.isEmpty()) {
            "No new orders was fetched"
        } else {
            buildString {
                append("Fetched ${orders.size}, ")
                append("listedAfter=$listedAfter (${listedAfter.epochSecond}), ")
                append("listedBefore=$listedBefore (${listedBefore.epochSecond}), ")
                append("new orders: ${orders.joinToString { it.hash.toString() }}")
            }
        }
        logger.looksrareInfo(logMessage)
    }

    private companion object {
        val logger by Logger()
    }
}