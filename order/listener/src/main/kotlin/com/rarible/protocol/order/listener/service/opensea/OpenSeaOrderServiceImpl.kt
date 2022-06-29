package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.opensea.client.OpenSeaClient
import com.rarible.opensea.client.SeaportProtocolClient
import com.rarible.opensea.client.model.OpenSeaError
import com.rarible.opensea.client.model.OperationResult
import com.rarible.opensea.client.model.v1.*
import com.rarible.opensea.client.model.v2.SeaportOrders
import com.rarible.opensea.client.model.v2.OrdersRequest as SeaportOrdersRequest
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil

@Component
@CaptureSpan(type = SpanType.EXT)
class OpenSeaOrderServiceImpl(
    private val openSeaClient: OpenSeaClient,
    private val seaportProtocolClient: SeaportProtocolClient,
    properties: OrderListenerProperties
) : OpenSeaOrderService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val loadOpenSeaOrderSide = convert(properties.openSeaOrderSide)
    private val seaportLoad = properties.seaportLoad

    override suspend fun getNextSellOrders(nextCursor: String?): SeaportOrders {
        val request = SeaportOrdersRequest(
            cursor = nextCursor,
            limit = MAX_SIZE
        )
        var lastError: OpenSeaError? = null
        var retries = 0

        while (retries++ < seaportLoad.retry) {
            when (val result = seaportProtocolClient.getListOrders(request)) {
                is OperationResult.Success -> return result.result
                is OperationResult.Fail -> lastError = result.error
            }
            delay(seaportLoad.retryDelay)
        }
        throw IllegalStateException("Can't fetch Seaport orders, number of attempts exceeded, last error: $lastError")
    }

    override suspend fun getNextOrdersBatch(
        listedAfter: Long,
        listedBefore: Long,
        loadPeriod: Duration,
        logPrefix: String,
    ): List<OpenSeaOrder> =
        coroutineScope {
            if (listedBefore == listedAfter) {
                emptyList()
            } else {
                val batches = ceil ((listedBefore - listedAfter).toDouble() / loadPeriod.seconds.toDouble()).toLong()
                assert(batches > 0) { "OpenSea batch count must be positive" }

                (1..batches).map {
                    async {
                        val nextListedAfter = listedAfter + ((it - 1) * loadPeriod.seconds)
                        val nextListedBefore = java.lang.Long.min(listedAfter + (it * loadPeriod.seconds), listedBefore)
                        getNextOrders(nextListedAfter, nextListedBefore, logPrefix)
                    }
                }.awaitAll().flatten()
            }
        }

    private suspend fun getNextOrders(listedAfter: Long, listedBefore: Long, logPrefix: String): List<OpenSeaOrder> {
        val orders = mutableListOf<OpenSeaOrder>()

        do {
            val request = OrdersRequest(
                listedAfter = Instant.ofEpochSecond(listedAfter),
                listedBefore = Instant.ofEpochSecond(listedBefore),
                offset = orders.size,
                sortBy = SortBy.CREATED_DATE,
                sortDirection = SortDirection.ASC,
                side = loadOpenSeaOrderSide,
                limit = MAX_SIZE
            )
            val result = getOrdersWithLogIfException(request, logPrefix)
            logger.info(
                "[$logPrefix] Load result: size=${result.size}, offset=${orders.size}, listedAfter=${listedAfter}, listedBefore=${listedBefore}"
            )
            orders.addAll(result)
        } while (result.isNotEmpty() && result.size >= MAX_SIZE && orders.size <= MAX_OFFSET)

        return orders
    }

    private suspend fun getOrders(request: OrdersRequest): List<OpenSeaOrder> {
        var lastError: OpenSeaError? = null
        var retries = 0

        while (retries++ < MAX_RETRIES) {
            when (val result = openSeaClient.getOrders(request)) {
                is OperationResult.Success -> return result.result.orders
                is OperationResult.Fail -> lastError = result.error
            }
            delay(Duration.ofMillis(300).multipliedBy(retries.toLong()))
        }
        throw IllegalStateException("Can't fetch OpenSea orders, number of attempts exceeded, last error: $lastError")
    }

    private suspend fun getOrdersWithLogIfException(request: OrdersRequest, logPrefix: String): List<OpenSeaOrder> {
        return try {
            getOrders(request)
        } catch (ex: Exception) {
            logger.error("[$logPrefix] Exception while get OpenSea orders with request: listedAfter=${request.listedAfter?.epochSecond}, listedBefore=${request.listedBefore?.epochSecond}, offset=${request.offset}, side=${request.side}, ex=${ex.javaClass.simpleName}")
            throw ex
        }
    }

    private fun convert(side: OrderListenerProperties.OrderSide?): OrderSide? {
        return when (side ?: OrderListenerProperties.OrderSide.ALL) {
            OrderListenerProperties.OrderSide.SELL -> OrderSide.SELL
            OrderListenerProperties.OrderSide.BID -> OrderSide.BUY
            OrderListenerProperties.OrderSide.ALL -> null
        }
    }

    companion object {
        const val MAX_SIZE = 50
        const val MAX_OFFSET = 10000
        const val MAX_RETRIES = 5
    }
}
