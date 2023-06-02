package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.looksrare.client.LooksrareClientV2
import com.rarible.looksrare.client.model.LooksrareError
import com.rarible.looksrare.client.model.OperationResult
import com.rarible.looksrare.client.model.v2.LooksrareOrder
import com.rarible.looksrare.client.model.v2.LooksrareOrders
import com.rarible.looksrare.client.model.v2.OrdersRequest
import com.rarible.looksrare.client.model.v2.Pagination
import com.rarible.looksrare.client.model.v2.QuoteType
import com.rarible.looksrare.client.model.v2.Sort
import com.rarible.protocol.order.core.model.LooksrareV2Cursor
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.listener.misc.looksrareInfo
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LooksrareOrderService(
    private val looksrareClient: LooksrareClientV2,
    private val properties: LooksrareLoadProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getNextSellOrders(cursor: LooksrareV2Cursor): List<LooksrareOrder> {
        val loadOrders = mutableSetOf<LooksrareOrder>()
        val createdAfter = cursor.createdAfter
        var nextId: String? = cursor.nextId
        var deep = 0
        do {
            val request = OrdersRequest(
                quoteType = QuoteType.ASK,
                status = null,
                sort = Sort.NEWEST,
                pagination = Pagination(first = properties.loadMaxSize, cursor = nextId)
            )
            val result = getOrders(request)
            loadOrders.addAll(result.data)

            val lastLoadOrder = result.data.lastOrNull()
            logger.looksrareInfo(
                "Load next: createdAfter=$createdAfter, cursor=$nextId, last=${lastLoadOrder?.createdAt}, deep=$deep"
            )
            nextId = lastLoadOrder?.id
            deep += 1
        } while (lastLoadOrder != null && lastLoadOrder.createdAt > createdAfter && deep < properties.loadMaxDeep)

        return loadOrders.toList()
    }

    private suspend fun getOrders(request: OrdersRequest): LooksrareOrders {
        var lastError: LooksrareError? = null
        var retries = 0

        while (retries < properties.retry) {
            when (val result = looksrareClient.getOrders(request)) {
                is OperationResult.Success -> return result.result
                is OperationResult.Fail -> lastError = result.error
            }
            retries += 1
            delay(properties.retryDelay)
        }
        throw IllegalStateException("Can't fetch Looksrare orders, number of attempts exceeded, last error: $lastError")
    }
}
