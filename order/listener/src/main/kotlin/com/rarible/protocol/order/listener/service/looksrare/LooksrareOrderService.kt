package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.looksrare.client.LooksrareClient
import com.rarible.looksrare.client.model.LooksrareError
import com.rarible.looksrare.client.model.OperationResult
import com.rarible.looksrare.client.model.v1.LooksrareOrder
import com.rarible.looksrare.client.model.v1.LooksrareOrders
import com.rarible.looksrare.client.model.v1.OrdersRequest
import com.rarible.looksrare.client.model.v1.Pagination
import com.rarible.looksrare.client.model.v1.Sort
import com.rarible.looksrare.client.model.v1.Status
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.listener.misc.LOOKSRARE_LOG
import com.rarible.protocol.order.listener.misc.looksrareInfo
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class LooksrareOrderService(
    private val looksrareClient: LooksrareClient,
    private val looksrareLoadCounter: RegisteredCounter,
    private val properties: LooksrareLoadProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getNextSellOrders(listedAfter: Instant, listedBefore: Instant): List<LooksrareOrder> {
        val loadOrders = mutableSetOf<LooksrareOrder>()
        var nextHash: Word? = null
        do {
            val request = OrdersRequest(
                isOrderAsk = true,
                startTime = listedBefore,
                endTime = null,
                status = listOf(Status.VALID),
                sort = Sort.NEWEST,
                pagination = Pagination(first = properties.loadMaxSize, cursor = nextHash?.prefixed())
            )
            logger.looksrareInfo(
                "Load next: startTime=${request.startTime?.epochSecond}, listedAfter=${listedAfter.epochSecond}, cursor=${request.pagination?.cursor}"
            )
            val result = getOrders(request)
            if (result.success.not()) throw IllegalStateException("$LOOKSRARE_LOG Can't load orders: ${result.message}")
            looksrareLoadCounter.increment(result.data.size)
            loadOrders.addAll(result.data)

            val lastLoadOrder = result.data.lastOrNull()
            logger.looksrareInfo("Last load order startTime ${lastLoadOrder?.startTime?.epochSecond}")
            nextHash = lastLoadOrder?.hash
        } while (lastLoadOrder != null && lastLoadOrder.startTime > listedAfter)

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
