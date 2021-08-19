package com.rarible.protocol.order.listener.service.opensea

import com.rarible.opensea.client.OpenSeaClient
import com.rarible.opensea.client.model.OpenSeaOrder
import com.rarible.opensea.client.model.OrdersRequest
import com.rarible.opensea.client.model.SortBy
import com.rarible.opensea.client.model.SortDirection
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
            val result = openSeaClient.getOrders(request).ensureSuccess().orders

            orders.addAll(result)
        } while (result.isNotEmpty() && orders.size <= MAX_OFFSET)

        return orders
    }

    companion object {
        const val MAX_OFFSET = 10000
    }
}

