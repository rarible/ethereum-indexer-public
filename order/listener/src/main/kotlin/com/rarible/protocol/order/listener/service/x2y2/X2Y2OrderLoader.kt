package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.X2Y2LoadProperties
import com.rarible.protocol.order.listener.misc.seaportInfo
import com.rarible.protocol.order.listener.misc.x2y2Error
import com.rarible.protocol.order.listener.misc.x2y2Info
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class X2Y2OrderLoader(
    private val x2y2OrderService: X2Y2OrderService,
    private val x2Y2OrderConverter: X2Y2OrderConverter,
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val properties: X2Y2LoadProperties,
    private val x2y2SaveCounter: RegisteredCounter
) {
    suspend fun load(cursor: String?): ApiListResponse<Order> {
        val result = safeGetNextSellOrders(cursor)
        val orders = result.data
        if (orders.isNotEmpty()) {
            val createdAts = orders.map { it.createdAt }
            val minCreatedAt = createdAts.minOrNull()
            val maxCreatedAt = createdAts.maxOrNull()

            logger.x2y2Info(
                buildString {
                    append("Fetched ${orders.size}, ")
                    append("minCreatedAt=$minCreatedAt, ")
                    append("maxCreatedAt=$maxCreatedAt, ")
                    append("cursor=$cursor, ")
                    append("new orders: ${orders.joinToString { it.itemHash.toString() }}")
                }
            )
            coroutineScope {
                @Suppress("ConvertCallChainIntoSequence")
                orders
                    .mapNotNull {
                        x2Y2OrderConverter.convert(it)
                    }
                    .chunked(properties.saveBatchSize)
                    .map { chunk ->
                        chunk.map {
                            async {
                                if (properties.saveEnabled && orderRepository.findById(it.hash) == null) {
                                    orderUpdateService.save(it)
                                    x2y2SaveCounter.increment()
                                    logger.x2y2Error("Saved new order ${it.hash}")
                                }
                            }
                        }.awaitAll()
                    }
                    .flatten()
                    .lastOrNull()
            }
        } else {
            logger.seaportInfo("No new orders was fetched")
        }
        return result
    }

    private suspend fun safeGetNextSellOrders(cursor: String?): ApiListResponse<Order> {
        return try {
            x2y2OrderService.getNextSellOrders(cursor)
        } catch (ex: Throwable) {
            logger.x2y2Error("Can't get next orders with cursor $cursor", ex)
            throw ex
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(X2Y2OrderLoader::class.java)
    }
}