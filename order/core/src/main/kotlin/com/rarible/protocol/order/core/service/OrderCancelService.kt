package com.rarible.protocol.order.core.service

import com.rarible.protocol.dto.offchainEventMark
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component

@Component
class OrderCancelService(
    private val orderStateRepository: OrderStateRepository,
    private val orderUpdateService: OrderUpdateService
) {
    suspend fun cancelOrder(orderHash: Word) {
        val markName = "api-cancel_order"
        val state = orderStateRepository.getById(orderHash) ?: OrderState(orderHash, false)
        orderStateRepository.save(state.withCanceled(true))
        orderUpdateService.update(orderHash, offchainEventMark(markName))
    }
}