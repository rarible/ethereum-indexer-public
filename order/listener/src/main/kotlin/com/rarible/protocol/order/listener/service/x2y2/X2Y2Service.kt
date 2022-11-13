package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.telemetry.metrics.RegisteredGauge
import com.rarible.x2y2.client.X2Y2ApiClient
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.Event
import com.rarible.x2y2.client.model.EventType
import com.rarible.x2y2.client.model.OperationResult
import com.rarible.x2y2.client.model.Order
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.time.Instant

@Component
class X2Y2Service(
    private val x2y2ApiClient: X2Y2ApiClient,
    private val x2y2OrderDelayGauge: RegisteredGauge<Long>,
    private val x2y2LoadCounter: RegisteredCounter,
    private val x2y2EventLoadCounter: RegisteredCounter,
    private val x2y2EventDelayGauge: RegisteredGauge<Long>,
) {
    suspend fun getNextSellOrders(nextCursor: String?): ApiListResponse<Order> {
        return fetch(
            fetcher = { x2y2ApiClient.orders(cursor = nextCursor) },
            createdAt = { it.createdAt },
            loadCounter = x2y2LoadCounter,
            delayGauge = x2y2OrderDelayGauge,
            entity = "'orders'"
        )
    }

    suspend fun getNextEvents(type: EventType, nextCursor: String?): ApiListResponse<Event> {
        return fetch(
            fetcher = { x2y2ApiClient.events(type = type, cursor = nextCursor) },
            createdAt = { it.createdAt },
            loadCounter = x2y2EventLoadCounter,
            delayGauge = x2y2EventDelayGauge,
            entity = "'${type.name} events'"
        )
    }

    suspend fun isActiveOrder(
        caller: Address,
        orderId: BigInteger,
        currency: Address,
        price: BigInteger,
        tokenId: BigInteger
    ): Boolean {
        val result = x2y2ApiClient.fetchOrderSign(
                caller = caller.prefixed(),
                op = BigInteger.ONE,
                orderId = orderId,
                currency = currency,
                price = price,
                tokenId = tokenId
            )
        return when (result) {
            is OperationResult.Success -> true

            is OperationResult.Fail -> {
                val code = result.error.errors.first().code
                if (code == 2020L) false else throw IllegalArgumentException("Can't determine invalid code, response: $result")
            }
        }
    }

    private suspend fun <T> fetch(
        fetcher: suspend () -> ApiListResponse<T>,
        createdAt: (T) -> Instant,
        loadCounter: RegisteredCounter,
        delayGauge: RegisteredGauge<Long>,
        entity: String
    ): ApiListResponse<T> {
        val result = fetcher()
        if (result.success) {
            loadCounter.increment(result.data.size)
            result.data.lastOrNull()?.let { delayGauge.set(nowMillis().epochSecond - createdAt(it).epochSecond) }
            return result
        }
        throw IllegalStateException("Can't fetch X2Y2 $entity, api return fail")
    }
}