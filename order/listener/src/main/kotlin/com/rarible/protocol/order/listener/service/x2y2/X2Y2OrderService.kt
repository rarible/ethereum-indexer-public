package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.telemetry.metrics.RegisteredGauge
import com.rarible.x2y2.client.X2Y2ApiClient
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.Order
import org.springframework.stereotype.Component

@Component
class X2Y2OrderService(
    private val x2y2ApiClient: X2Y2ApiClient,
    private val x2y2OrderDelayGauge: RegisteredGauge<Long>,
    private val x2y2LoadCounter: RegisteredCounter
) {
    suspend fun getNextSellOrders(nextCursor: String?): ApiListResponse<Order> {
        val result = x2y2ApiClient.orders(cursor = nextCursor)
        if (result.success) {
            x2y2LoadCounter.increment(result.data.size)
            result.data.lastOrNull()?.let {  x2y2OrderDelayGauge.set(nowMillis().epochSecond - it.createdAt.epochSecond) }
            return result
        }
        throw IllegalStateException("Can't fetch X2Y2 orders, api return fail")
    }
}