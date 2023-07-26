package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.EventTimeMarks
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.misc.orderTaskEventMarks
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.itemId
import com.rarible.protocol.order.core.model.nft
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.updater.CancelInactiveOrderUpdater
import com.rarible.protocol.order.core.service.x2y2.X2Y2Service
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import org.springframework.stereotype.Component

@Component
class OrderX2Y2StatusTaskHandler(
    orderRepository: OrderRepository,
    properties: OrderListenerProperties,
    private val orderUpdateService: OrderUpdateService,
    private val x2y2Service: X2Y2Service,
    private val cancelInactiveOrderUpdater: CancelInactiveOrderUpdater,
) : AbstractOrderUpdateStatusTaskHandler(orderRepository, properties) {

    override val type: String
        get() = UPDATE_X2Y2_ORDER

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override suspend fun handleOrder(order: Order) {
        val eventTimeMarks = orderTaskEventMarks()
        val isActive = x2y2Service.isActiveOrder(order)
        if (isActive.not() && properties.fixX2Y2) {
            logger.info(
                "Detected not active x2y2 order ${order.hash}, itemId=${order.nft().type.itemId}"
            )
            cancelX2Y2(order, eventTimeMarks)
        }
    }

    private suspend fun cancelX2Y2(order: Order, eventTimeMarks: EventTimeMarks) {
        val inactiveOrder = order.withMakeBalance(EthUInt256.ZERO, EthUInt256.ZERO)
        require(inactiveOrder.status == OrderStatus.INACTIVE) { "Not INACTIVE status for order: $inactiveOrder" }
        require(inactiveOrder.platform == Platform.X2Y2) { "Not X2Y2 order: $inactiveOrder" }

        cancelInactiveOrderUpdater.update(inactiveOrder)
        orderUpdateService.update(inactiveOrder.hash, eventTimeMarks)

        val updated = orderRepository.findById(inactiveOrder.hash)
        logger.info("X2Y2 order status was updated: hash={}, status={}", updated?.hash, updated?.status)
    }

    companion object {
        const val UPDATE_X2Y2_ORDER = "UPDATE_X2Y2_ORDER"
    }
}
