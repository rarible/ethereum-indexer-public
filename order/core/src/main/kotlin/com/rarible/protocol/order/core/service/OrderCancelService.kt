package com.rarible.protocol.order.core.service

import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.Order
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component

@Component
class OrderCancelService(
    private val orderStateService: OrderStateService,
    private val orderUpdateService: OrderUpdateService,
    private val orderVersionCleanSignatureService: OrderVersionCleanSignatureService
) {
    suspend fun cancelOrder(
        id: Word,
        eventTimeMarksDto: EventTimeMarks = orderOffchainEventMarks()
    ): Order? {
        orderStateService.setCancelState(id)
        orderVersionCleanSignatureService.cleanSignature(id)
        return orderUpdateService.update(id, eventTimeMarksDto)
    }
}
