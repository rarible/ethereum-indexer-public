package com.rarible.protocol.order.api.controller

import com.rarible.core.logging.RaribleMDCContext
import com.rarible.protocol.dto.CreateTransactionRequestDto
import com.rarible.protocol.dto.LogEventDto
import com.rarible.protocol.order.api.service.pending.PendingTransactionService
import com.rarible.protocol.order.core.converters.dto.TransactionDtoConverter
import com.rarible.protocol.order.core.converters.model.ListenerTransactionConverter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class TransactionController(
    private val pendingTransactionService: PendingTransactionService
) : OrderTransactionControllerApi {

    override fun createOrderPendingTransaction(request: CreateTransactionRequestDto): ResponseEntity<Flow<LogEventDto>> {

        val transaction = ListenerTransactionConverter.convert(request)

        val result = flow {
            pendingTransactionService
                .process(transaction)
                .forEach { emit(TransactionDtoConverter.convert(it)) }
        }.flowOn(RaribleMDCContext())

        return ResponseEntity.ok(result)
    }
}
