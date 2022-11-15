package com.rarible.protocol.order.listener.service.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.currency
import com.rarible.protocol.order.core.model.isBid
import com.rarible.protocol.order.core.model.isSell
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.model.tokenId
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.updater.X2Y2OrderUpdater
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.service.x2y2.X2Y2Service
import org.springframework.stereotype.Component
import java.lang.IllegalStateException

@Component
class OrderX2Y2StatusTaskHandler(
    orderRepository: OrderRepository,
    properties: OrderListenerProperties,
    private val orderUpdateService: OrderUpdateService,
    private val x2y2Service: X2Y2Service,
    private val x2Y2OrderUpdater: X2Y2OrderUpdater,
) : AbstractOrderUpdateStatusTaskHandler(orderRepository, properties) {

    override val type: String
        get() = UPDATE_X2Y2_ORDER

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
            tokenId = tokenId?.value ?: throw IllegalStateException("Can't get tokenId for order: ${order.hash}"),
        )
        if (isActive.not() && properties.fixX2Y2) {
            logger.info(
                "Detected not active x2y2 order ${order.hash}, collection=${order.token}, tokeId=${tokenId}"
            )
            cancelX2Y2(order)
        }
    }

    private suspend fun cancelX2Y2(order: Order) {
        val inactiveOrder = order.withMakeBalance(EthUInt256.ZERO, EthUInt256.ZERO)
        require(inactiveOrder.status == OrderStatus.INACTIVE) { "Not INACTIVE status for order: $inactiveOrder" }
        require(inactiveOrder.platform == Platform.X2Y2) { "Not X2Y2 order: $inactiveOrder" }

        x2Y2OrderUpdater.update(inactiveOrder)
        orderUpdateService.update(inactiveOrder.hash)

        val updated = orderRepository.findById(inactiveOrder.hash)
        logger.info("X2Y2 order status was updated: hash={}, status={}", updated?.hash, updated?.status)
    }

    companion object {
        const val UPDATE_X2Y2_ORDER = "UPDATE_X2Y2_ORDER"
    }
}