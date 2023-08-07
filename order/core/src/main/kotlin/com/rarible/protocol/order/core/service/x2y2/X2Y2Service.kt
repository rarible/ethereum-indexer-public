package com.rarible.protocol.order.core.service.x2y2

import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics.ApiCallStatus
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.currency
import com.rarible.protocol.order.core.model.nft
import com.rarible.protocol.order.core.model.payment
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.model.tokenId
import com.rarible.protocol.order.core.service.OrderStateCheckService
import com.rarible.x2y2.client.X2Y2ApiClient
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.Event
import com.rarible.x2y2.client.model.EventType
import com.rarible.x2y2.client.model.OperationResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger
import com.rarible.x2y2.client.model.Order as X2Y2Order

@Component
class X2Y2Service(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val x2y2ApiClient: X2Y2ApiClient,
    private val metrics: ForeignOrderMetrics,
) : OrderStateCheckService {
    suspend fun getNextSellOrders(nextCursor: String?): ApiListResponse<X2Y2Order> {
        val result = x2y2ApiClient.orders(cursor = nextCursor)
        if (!result.success) {
            onCallForeignApi(ApiCallStatus.FAIL)
            throw IllegalStateException("Can't fetch X2Y2 'orders', api return fail")
        }
        onCallForeignApi(ApiCallStatus.OK)
        return result
    }

    suspend fun getNextEvents(type: EventType, nextCursor: String?): ApiListResponse<Event> {
        val result = x2y2ApiClient.events(type = type, cursor = nextCursor)
        if (!result.success) {
            onCallForeignApi(ApiCallStatus.FAIL)
            throw IllegalStateException("Can't fetch X2Y2 '${type.name} events', api return fail")
        }
        onCallForeignApi(ApiCallStatus.OK)
        return result
    }

    override suspend fun isActiveOrder(order: Order): Boolean {
        val data = order.data as? OrderX2Y2DataV1 ?: run {
            logger.error("Invalid order data (not x2y2 data): hash={}", order.hash)
            return true
        }
        val price = order.payment().value
        val tokenId = order.nft().type.tokenId

        return isActiveOrder(
            caller = order.maker,
            orderId = data.orderId,
            currency = order.currency.token,
            price = price.value,
            tokenId = tokenId?.value ?: throw IllegalStateException("Can't get tokenId for order: ${order.hash}"),
        )
    }

    private suspend fun isActiveOrder(
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

    private fun onCallForeignApi(status: ApiCallStatus) {
        metrics.onCallForeignOrderApi(Platform.X2Y2, status)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(X2Y2Service::class.java)
    }
}
