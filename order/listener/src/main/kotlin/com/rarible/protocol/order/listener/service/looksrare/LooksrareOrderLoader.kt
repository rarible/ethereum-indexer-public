package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.common.flatMapAsync
import com.rarible.core.logging.Logger
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.looksrare.client.model.v1.LooksRareOrders
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties

class LooksrareOrderLoader(
    private val looksrareOrderService: LooksrareOrderService,
    private val looksrareOrderConverter: LooksrareOrderConverter,
    private val looksrareOrderValidator: LooksrareOrderValidator,
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val properties: LooksrareLoadProperties,
    private val looksrareSaveCounter: RegisteredCounter
) {
    suspend fun load(cursor: String?): LooksRareOrders {
        val result = safeGetNextSellOrders(cursor)
        val orders = result.data
        if (orders.isEmpty()) {
            logger.info("[Looksrare] No new orders was fetched")
        }
        val createdAts = orders.map { it.startTime }
        val minCreatedAt = createdAts.minOrNull()
        val maxCreatedAt = createdAts.maxOrNull()

        logger.info(
            buildString {
                append("[Looksrare] Fetched ${orders.size}, ")
                append("minCreatedAt=$minCreatedAt, ")
                append("maxCreatedAt=$maxCreatedAt, ")
                append("cursor=$cursor, ")
                append("new orders: ${orders.joinToString { it.hash.toString() }}")
            }
        )

        orders
            .mapNotNull{ looksrareOrderConverter.convertOrder(it) }
            .filter(looksrareOrderValidator::validate)
            .chunked(properties.saveBatchSize)
            .flatMapAsync { chunk ->
                chunk.map {
                    if (properties.saveEnabled && orderRepository.findById(it.hash) == null) {
                        orderUpdateService.save(it)
                        looksrareSaveCounter.increment()
                        logger.info("[Looksrare] Saved new order ${it.hash}")
                    }
                }
            }


        return result
    }

    private suspend fun safeGetNextSellOrders(cursor: String?): LooksRareOrders {
        return try {
            looksrareOrderService.getNextSellOrders(cursor)
        } catch (ex: Throwable) {
            logger.error("[Looksrare] Can't get next orders with cursor $cursor", ex)
            throw ex
        }
    }

    private companion object {
        val logger by Logger()
    }
}