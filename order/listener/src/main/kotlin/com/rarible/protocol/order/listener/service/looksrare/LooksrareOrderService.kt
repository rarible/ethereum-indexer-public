package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.looksrare.client.LooksrareClient
import com.rarible.looksrare.client.model.v1.LooksrareOrder
import com.rarible.looksrare.client.model.v1.OrdersRequest
import com.rarible.looksrare.client.model.v1.Pagination
import com.rarible.looksrare.client.model.v1.Sort
import com.rarible.looksrare.client.model.v1.Status
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.listener.misc.LOOKSRARE_LOG
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class LooksrareOrderService(
    private val looksrareClient: LooksrareClient,
    private val looksrareLoadCounter: RegisteredCounter,
    private val properties: LooksrareLoadProperties
) {
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
            val result = looksrareClient.getOrders(request).ensureSuccess()
            if (result.success.not()) throw IllegalStateException("$LOOKSRARE_LOG Can't load orders: ${result.message}")
            looksrareLoadCounter.increment(result.data.size)
            loadOrders.addAll(result.data)

            val lastLoadOrder = result.data.lastOrNull()
            nextHash = lastLoadOrder?.hash
        } while (lastLoadOrder != null && lastLoadOrder.startTime > listedAfter)

        return loadOrders.toList()
    }
}
