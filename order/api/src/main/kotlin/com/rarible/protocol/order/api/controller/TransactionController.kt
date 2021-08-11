package com.rarible.protocol.order.api.controller


import com.rarible.core.common.convert
import com.rarible.core.logging.RaribleMDCContext
import com.rarible.ethereum.log.domain.TransactionDto
import com.rarible.protocol.dto.CreateTransactionRequestDto
import com.rarible.protocol.dto.LogEventDto
import com.rarible.protocol.order.api.service.pending.PendingTransactionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.springframework.core.convert.ConversionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class TransactionController(
    private val conversionService: ConversionService,
    private val pendingTransactionService: PendingTransactionService
) : OrderTransactionControllerApi {

    override fun createOrderPendingTransaction(request: CreateTransactionRequestDto): ResponseEntity<Flow<LogEventDto>> {

        val transaction = conversionService.convert<TransactionDto>(request)

        val result = flow<LogEventDto> {
            pendingTransactionService
                .process(transaction)
                .forEach { emit(conversionService.convert(it)) }
        }.flowOn(RaribleMDCContext())

        return ResponseEntity.ok(result)
    }
}
