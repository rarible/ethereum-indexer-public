package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import com.rarible.core.common.EventTimeMarks

@Component
class OrderCancelService(
    private val orderStateService: OrderStateService,
    private val orderUpdateService: OrderUpdateService,
    private val orderVersionCleanSignatureService: OrderVersionCleanSignatureService
) {
    suspend fun cancelOrder(
        id: Word,
        eventTimeMarksDto: EventTimeMarks = orderOffchainEventMarks()
    ) {
        orderStateService.setCancelState(id)
        orderVersionCleanSignatureService.cleanSignature(id)
        orderUpdateService.update(id, eventTimeMarksDto)
    }
}