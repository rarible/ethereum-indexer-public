package com.rarible.protocol.order.core.service.updater

import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import org.springframework.stereotype.Component

@Component
class X2Y2OrderUpdater(
    private val orderStateRepository: OrderStateRepository
) : CustomOrderUpdater {

    override suspend fun update(order: Order): Order {
        // For x2y2 we want to set all INACTIVE orders as cancelled, they can't become ACTIVE again
        if (order.platform != Platform.X2Y2 || order.status != OrderStatus.INACTIVE) {
            return order
        }
        // Saving this state to keep order cancelled after future reduces
        val state = orderStateRepository.getById(order.hash) ?: OrderState(order.hash, false)
        val updatedState = orderStateRepository.save(state.withCanceled(true))

        return order.withFinalState(updatedState)
    }
}