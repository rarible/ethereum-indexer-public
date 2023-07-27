package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.protocol.order.core.misc.orderIntegrationEventMarks
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.x2y2.X2Y2Service
import com.rarible.protocol.order.listener.configuration.X2Y2OrderLoadProperties
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.protocol.order.listener.misc.seaportInfo
import com.rarible.protocol.order.listener.misc.x2y2Error
import com.rarible.protocol.order.listener.misc.x2y2Info
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class X2Y2OrderLoader(
    private val x2y2OrderService: X2Y2Service,
    private val x2Y2OrderConverter: X2Y2OrderConverter,
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val properties: X2Y2OrderLoadProperties,
    private val metrics: ForeignOrderMetrics
) {

    private val logger = LoggerFactory.getLogger(javaClass)

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
                                    val eventTimeMarks = orderIntegrationEventMarks(it.createdAt)
                                    // TODO 2 events will be emitted here - is it fine?
                                    orderUpdateService.save(it, eventTimeMarks).also {
                                        orderUpdateService.updateMakeStock(it, null, eventTimeMarks)
                                    }
                                    metrics.onDownloadedOrderHandled(Platform.X2Y2)
                                    logger.x2y2Info("Saved new order ${it.hash}")
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
            val orders = x2y2OrderService.getNextSellOrders(cursor)
            orders.data.forEach { metrics.onOrderReceived(Platform.X2Y2, it.createdAt) }
            orders.data.maxOfOrNull { it.createdAt }?.let {
                metrics.onLatestOrderReceived(Platform.X2Y2, it)
            }
            orders
        } catch (ex: Throwable) {
            logger.x2y2Error("Can't get next orders with cursor $cursor", ex)
            throw ex
        }
    }
}
