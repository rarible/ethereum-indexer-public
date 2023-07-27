package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component

@Component
class OrderStateService(
    private val orderStateRepository: OrderStateRepository
) {

    suspend fun setCancelState(id: Word): OrderState {
        val current = orderStateRepository.getById(id) ?: OrderState.default(id)
        return if (!current.canceled) {
            orderStateRepository.save(current.withCanceled(true))
        } else {
            current
        }
    }
}
