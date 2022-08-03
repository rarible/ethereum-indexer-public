package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.common.flatMapAsync
import com.rarible.core.logging.Logger
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.looksrare.client.model.v1.LooksrareOrder
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.listener.misc.looksrareError
import com.rarible.protocol.order.listener.misc.looksrareInfo
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class LooksrareOrderLoader(
    private val looksrareOrderService: LooksrareOrderService,
    private val looksrareOrderConverter: LooksrareOrderConverter,
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val properties: LooksrareLoadProperties,
    private val looksrareSaveCounter: RegisteredCounter
) {
    suspend fun load(
        listedAfter: Instant,
        listedBefore: Instant
    ): List<LooksrareOrder> {
        val orders = safeGetNextSellOrders(listedAfter, listedBefore)
        logOrderLoad(orders, listedAfter, listedBefore)

        orders
            .mapNotNull{ looksrareOrderConverter.convertOrder(it) }
            .chunked(properties.saveBatchSize)
            .flatMapAsync { chunk ->
                chunk.map {
                    if (properties.saveEnabled && orderRepository.findById(it.hash) == null) {
                        orderUpdateService.save(it)
                        looksrareSaveCounter.increment()
                        logger.looksrareInfo("Saved new order ${it.hash}")
                    }
                }
            }.lastOrNull()

        return orders
    }

    private suspend fun safeGetNextSellOrders(listedAfter: Instant, listedBefore: Instant): List<LooksrareOrder> {
        return try {
            looksrareOrderService.getNextSellOrders(listedAfter, listedBefore)
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