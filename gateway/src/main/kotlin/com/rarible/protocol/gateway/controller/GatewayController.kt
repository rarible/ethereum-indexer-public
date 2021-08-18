package com.rarible.protocol.gateway.controller

import com.rarible.core.logging.RaribleMDCContext
import com.rarible.protocol.dto.CreateTransactionRequestDto
import com.rarible.protocol.dto.LogEventDto
import com.rarible.protocol.gateway.api.controller.GatewayControllerApi
import com.rarible.protocol.gateway.configuration.GatewayProperties
import com.rarible.protocol.gateway.service.transaction.TransactionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class GatewayController(
    private val properties: GatewayProperties,
    private val transactionService: TransactionService
) : GatewayControllerApi {
    private val blockchain = properties.blockchain

    override fun createGatewayPendingTransactions(
        createTransactionRequestDto: CreateTransactionRequestDto
    ): ResponseEntity<Flow<LogEventDto>> {
        val result = flow<LogEventDto> {
            transactionService.createPendingTransaction(blockchain, createTransactionRequestDto)
        }.flowOn(RaribleMDCContext())
        return ResponseEntity.ok(result)
    }
}
