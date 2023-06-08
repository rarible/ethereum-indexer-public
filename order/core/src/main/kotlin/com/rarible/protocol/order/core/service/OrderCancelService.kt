package com.rarible.protocol.order.core.service

import com.rarible.protocol.dto.EventTimeMarksDto
import com.rarible.protocol.dto.offchainEventMark
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
        eventTimeMarksDto: EventTimeMarksDto = offchainEventMark("indexer-in_order")
    ) {
        orderStateService.setCancelState(id)
        orderVersionCleanSignatureService.cleanSignature(id)
        orderUpdateService.update(id, eventTimeMarksDto)
    }
}