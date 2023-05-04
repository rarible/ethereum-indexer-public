package com.rarible.protocol.order.core.service.updater

import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.OrderStateService
import org.springframework.stereotype.Component

@Component
class CancelInactiveOrderUpdater(
    private val orderStateService: OrderStateService
) : CustomOrderUpdater {

    private val platforms = setOf(Platform.X2Y2, Platform.OPEN_SEA)

    override suspend fun update(order: Order): Order {
        // For x2y2 and OpenSea we want to set all INACTIVE orders as cancelled, they can't become ACTIVE again
        if (!platforms.contains(order.platform) || order.status != OrderStatus.INACTIVE) {
            return order
        }
        // Saving this state to keep order cancelled after future reduces
        val state = orderStateService.setCancelState(order.hash)
        return order.withFinalState(state)
    }
}
