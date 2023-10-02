package com.rarible.protocol.order.listener.service.opensea

import com.rarible.opensea.client.SeaportProtocolClient
import com.rarible.opensea.client.model.OpenSeaError
import com.rarible.opensea.client.model.OperationResult
import com.rarible.opensea.client.model.v2.SeaportOrders
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.order.logger
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.time.delay
import org.springframework.stereotype.Component
import kotlin.math.max
import com.rarible.opensea.client.model.v2.OrdersRequest as SeaportOrdersRequest

@Component
class OpenSeaOrderServiceImpl(
    private val seaportRequestCursorProducer: SeaportRequestCursorProducer,
    private val seaportProtocolClient: SeaportProtocolClient,
    private val seaportLoad: SeaportLoadProperties,
    private val metrics: ForeignOrderMetrics
) : OpenSeaOrderService {

    override suspend fun getNextSellOrders(nextCursor: String?, loadAhead: Boolean): SeaportOrders {
        val requests = mutableListOf<SeaportOrdersRequest>()
        requests.add(
            SeaportOrdersRequest(
                cursor = nextCursor,
                limit = seaportLoad.loadMaxSize
            )
        )
        if (nextCursor != null && loadAhead) {
            seaportRequestCursorProducer.produceNextFromCursor(
                cursor = nextCursor,
                step = seaportLoad.loadMaxSize,
                amount = max(0, seaportLoad.maxAsyncRequests - 1)
            ).forEach { cursor ->
                requests.add(
                    SeaportOrdersRequest(
                        cursor = cursor,
                        limit = seaportLoad.loadMaxSize
                    )
                )
            }
        }
        val result = coroutineScope {
            val results = requests.map { async { getOrders(it) } }.awaitAll()
            when (results.size) {
                0 -> throw IllegalStateException("Unexpected results size for $nextCursor")
                1 -> results.single()
                else -> {
                    val orders = results.map { it.orders }.flatten().distinct()
                    val result = results.lastWithPreviousCursor() ?: results.first()
                    result.copy(orders = orders)
                }
            }
        }
        result.orders.forEach { metrics.onOrderReceived(Platform.OPEN_SEA, it.createdAt) }
        result.orders.maxOfOrNull { it.createdAt }?.let { metrics.onLatestOrderReceived(Platform.OPEN_SEA, it) }
        return result
    }

    private suspend fun getOrders(request: SeaportOrdersRequest): SeaportOrders {
        var lastError: OpenSeaError? = null
        var retries = 0

        while (retries < seaportLoad.retry) {
            logger.info("Seaport api request: cursor={}", request.cursor)
            when (val result = seaportProtocolClient.getListOrders(request)) {
                is OperationResult.Success -> return result.result
                is OperationResult.Fail -> lastError = result.error
            }
            retries += 1
            delay(seaportLoad.retryDelay)
        }
        throw IllegalStateException("Can't fetch Seaport orders, number of attempts exceeded, last error: $lastError")
    }

    private fun List<SeaportOrders>.lastWithPreviousCursor(): SeaportOrders? {
        var lastSeenWithPrevias: SeaportOrders? = null
        for (result in this) {
            if (result.previous != null)
                lastSeenWithPrevias = result
            if (result.previous == null)
                return lastSeenWithPrevias
        }
        return lastSeenWithPrevias
    }
}
