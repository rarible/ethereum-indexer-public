package com.rarible.protocol.order.core.service

import com.rarible.core.common.EventTimeMarks
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component

@Component
class OrderCancelService(
    private val orderStateService: OrderStateService,
    private val orderUpdateService: OrderUpdateService
) {

    suspend fun cancelOrder(id: Word, eventTimeMarksDto: EventTimeMarks) {
        orderStateService.setCancelState(id)
        orderUpdateService.update(id, eventTimeMarksDto)
    }
}