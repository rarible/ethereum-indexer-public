package com.rarible.protocol.order.api.controller

import com.rarible.protocol.dto.CreateTransactionRequestDto
import com.rarible.protocol.dto.LogEventDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class TransactionController : OrderTransactionControllerApi {
    override fun createOrderPendingTransaction(request: CreateTransactionRequestDto): ResponseEntity<Flow<LogEventDto>> {
        val empty = flow<LogEventDto> { }
        return ResponseEntity.ok(empty)
    }
}
