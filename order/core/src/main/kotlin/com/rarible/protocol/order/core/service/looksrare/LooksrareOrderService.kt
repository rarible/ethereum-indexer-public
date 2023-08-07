package com.rarible.protocol.order.core.service.looksrare

import com.rarible.looksrare.client.LooksrareClientV2
import com.rarible.looksrare.client.model.LooksrareError
import com.rarible.looksrare.client.model.OperationResult
import com.rarible.looksrare.client.model.v2.LooksrareOrder
import com.rarible.looksrare.client.model.v2.LooksrareOrders
import com.rarible.looksrare.client.model.v2.OrdersRequest
import com.rarible.looksrare.client.model.v2.Pagination
import com.rarible.looksrare.client.model.v2.QuoteType
import com.rarible.looksrare.client.model.v2.Sort
import com.rarible.looksrare.client.model.v2.Status
import com.rarible.protocol.order.core.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics.ApiCallStatus
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import com.rarible.protocol.order.core.misc.looksrareInfo
import com.rarible.protocol.order.core.model.LooksrareV2Cursor
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.isSell
import com.rarible.protocol.order.core.model.nft
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.model.tokenId
import com.rarible.protocol.order.core.service.OrderStateCheckService
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Component
class LooksrareOrderService(
    private val looksrareClient: LooksrareClientV2,
    private val properties: LooksrareLoadProperties,
    private val metrics: ForeignOrderMetrics,
) : OrderStateCheckService {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getNextSellOrders(cursor: LooksrareV2Cursor): List<LooksrareOrder> {
        val loadOrders = ConcurrentLinkedQueue<LooksrareOrder>()
        val createdAfter = cursor.createdAfter
        val nextId = AtomicReference(cursor.nextId)
        val deep = AtomicInteger()
        do {
            val request = OrdersRequest(
                quoteType = QuoteType.ASK,
                status = Status.VALID,
                sort = Sort.NEWEST,
                pagination = Pagination(first = properties.loadMaxSize, cursor = nextId.get())
            )
            val result = getOrders(request)
            loadOrders.addAll(result.data)

            val lastLoadOrder = result.data.lastOrNull()
            logger.looksrareInfo(
                "Load next: createdAfter=$createdAfter, cursor=$nextId, last=${lastLoadOrder?.createdAt}, deep=$deep"
            )
            nextId.set(lastLoadOrder?.id)
        } while (
            lastLoadOrder != null &&
            lastLoadOrder.createdAt > createdAfter &&
            deep.incrementAndGet() < properties.loadMaxDeep
        )

        return loadOrders.toSet().toList()
    }

    private suspend fun getOrders(request: OrdersRequest): LooksrareOrders {
        val lastError = AtomicReference<LooksrareError>()
        val retries = AtomicInteger()

        while (retries.get() < properties.retry) {
            when (val result = looksrareClient.getOrders(request)) {
                is OperationResult.Success -> {
                    onCallForeignApi(ApiCallStatus.OK)
                    return result.result
                }
                is OperationResult.Fail -> {
                    onCallForeignApi(ApiCallStatus.FAIL)
                    lastError.set(result.error)
                }
            }
            retries.incrementAndGet()
            delay(properties.retryDelay)
        }
        throw IllegalStateException("Can't fetch Looksrare orders, number of attempts exceeded, last error: $lastError")
    }

    override suspend fun isActiveOrder(order: Order): Boolean {
        val nft = order.nft()
        val tokenId = nft.type.tokenId?.value?.toString()
            ?: throw IllegalStateException("Can't get tokenId for order: ${order.hash}")
        val nextId = AtomicReference<String>()
        repeat(properties.loadMaxDeep) {
            val orders = getOrders(
                OrdersRequest(
                    collection = nft.type.token,
                    itemId = tokenId,
                    quoteType = if (order.isSell()) QuoteType.ASK else QuoteType.BID,
                    status = Status.VALID,
                    sort = Sort.NEWEST,
                    pagination = Pagination(first = properties.loadMaxSize, cursor = nextId.get())
                )
            ).data
            if (orders.any { it.hash == order.hash }) {
                return true
            }
            if (orders.isEmpty()) {
                return false
            }
            nextId.set(orders.last().id)
        }
        return true
    }

    private fun onCallForeignApi(status: ApiCallStatus) {
        metrics.onCallForeignOrderApi(Platform.LOOKSRARE, status)
    }
}
