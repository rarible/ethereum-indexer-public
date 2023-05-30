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
import com.rarible.looksrare.client.model.v2.Status
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.listener.misc.LOOKSRARE_LOG
import com.rarible.protocol.order.listener.misc.looksrareInfo
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class LooksrareOrderService(
    private val looksrareClient: LooksrareClientV2,
    private val properties: LooksrareLoadProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getNextSellOrders(createdAfter: Instant): List<LooksrareOrder> {
        val loadOrders = mutableSetOf<LooksrareOrder>()
        var nextId: String? = null
        do {
            val request = OrdersRequest(
                quoteType = QuoteType.ASK,
                status = null,
                sort = Sort.NEWEST,
                pagination = Pagination(first = properties.loadMaxSize, cursor = nextId)
            )
            logger.looksrareInfo(
                "Load next: createdAfter=$createdAfter, cursor=${request.pagination?.cursor}"
            )
            val result = getOrders(request)
            if (result.success.not()) throw IllegalStateException("$LOOKSRARE_LOG Can't load orders: ${result.message}")
            loadOrders.addAll(result.data)

            val lastLoadOrder = result.data.lastOrNull()
            logger.looksrareInfo("Last load order created time ${lastLoadOrder?.createdAt}")
            nextId = lastLoadOrder?.id
        } while (lastLoadOrder != null && lastLoadOrder.createdAt > createdAfter)

        return loadOrders.toList().filter { it.status == Status.VALID }
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
