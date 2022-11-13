package com.rarible.protocol.order.listener.service.order

import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1
import com.rarible.protocol.order.core.model.currency
import com.rarible.protocol.order.core.model.isBid
import com.rarible.protocol.order.core.model.isSell
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.model.tokenId
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.service.x2y2.X2Y2Service
import org.springframework.stereotype.Component
import java.lang.IllegalStateException

@Component
class OrderX2Y2StatusTaskHandler(
    orderRepository: OrderRepository,
    properties: OrderListenerProperties,
    private val x2y2Service: X2Y2Service,
) : AbstractOrderUpdateStatusTaskHandler(orderRepository, properties) {

    override val type: String
        get() = UPDATE_ORDER_APPROVAL

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override suspend fun handleOrder(order: Order) {
        val data = order.data as? OrderX2Y2DataV1 ?: run {
            logger.error("Invalid order data (not x2y2 data): hash={}", order.hash)
            return
        }
        val (price, tokenId) = when {
            order.isSell() -> {
                val tokenId = order.make.type.tokenId
                val price = order.take.value
                price to tokenId
            }
            order.isBid() -> {
                val price = order.make.value
                val tokenId = order.take.type.tokenId
                price to tokenId
            }
            else -> throw IllegalStateException("Can't get price for order: ${order.hash}")
        }
        val isActive = x2y2Service.isActiveOrder(
            caller = order.maker,
            orderId = data.orderId,
            currency = order.currency.token,
            price = price.value,
            tokenId = tokenId.value,
        )
        if (isActive && properties.fixX2Y2) {
            cancelX2Y2(order)
        }
    }

    private suspend fun cancelX2Y2(order: Order) {
        order.withMakeBalance(§ )
    }

    companion object {
        const val UPDATE_ORDER_APPROVAL = "UPDATE_ORDER_APPROVAL"
    }
}