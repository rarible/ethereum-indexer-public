package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.x2y2.client.X2Y2ApiClient
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.Event
import com.rarible.x2y2.client.model.EventType
import com.rarible.x2y2.client.model.OperationResult
import com.rarible.x2y2.client.model.Order
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger

@Component
class X2Y2Service(
    private val x2y2ApiClient: X2Y2ApiClient
) {
    suspend fun getNextSellOrders(nextCursor: String?): ApiListResponse<Order> {
        val result = x2y2ApiClient.orders(cursor = nextCursor)
        if (!result.success) throw IllegalStateException("Can't fetch X2Y2 'orders', api return fail")
        return result
    }

    suspend fun getNextEvents(type: EventType, nextCursor: String?): ApiListResponse<Event> {
        val result = x2y2ApiClient.events(type = type, cursor = nextCursor)
        if (!result.success) throw IllegalStateException("Can't fetch X2Y2 '${type.name} events', api return fail")
        return result
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
}