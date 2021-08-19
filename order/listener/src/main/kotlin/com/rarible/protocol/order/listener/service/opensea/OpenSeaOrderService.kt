package com.rarible.protocol.order.listener.service.opensea

import com.rarible.opensea.client.OpenSeaClient
import com.rarible.opensea.client.model.*
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class OpenSeaOrderService(
    private val openSeaClient: OpenSeaClient
) {
    suspend fun getNextOrders(listedAfter: Long, listedBefore: Long): List<OpenSeaOrder> {
        val orders = mutableListOf<OpenSeaOrder>()

        do {
            val request = OrdersRequest(
                listedAfter = Instant.ofEpochSecond(listedAfter),
                listedBefore = Instant.ofEpochSecond(listedBefore),
                offset = orders.size,
                sortBy = SortBy.CREATED_DATE,
                sortDirection = SortDirection.ASC,
                limit = null,
                side = null
            )
            val result = getOrders(request)

            orders.addAll(result)
        } while (result.isNotEmpty() && orders.size <= MAX_OFFSET)

        return orders
    }

    private suspend fun getOrders(request: OrdersRequest): List<OpenSeaOrder> {
        return when (val result = openSeaClient.getOrders(request)) {
            is OperationResult.Success -> result.result.orders
            is OperationResult.Fail -> when (result.error.code) {
                OpenSeaErrorCode.TOO_MANY_REQUESTS -> emptyList()
                else -> throw IllegalStateException("Operation finished with error ${result.error}")
            }
        }
    }

    companion object {
        const val MAX_OFFSET = 10000
    }
}

