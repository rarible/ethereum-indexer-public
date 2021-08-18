package com.rarible.protocol.gateway.service.transaction

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.CreateTransactionRequestDto
import com.rarible.protocol.dto.LogEventDto
import com.rarible.protocol.gateway.service.cluster.IndexerApiClientProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component

@Component
class TransactionService(
    private val indexerApiClientFactory: IndexerApiClientProvider
) {
    suspend fun createPendingTransaction(blockchain: Blockchain, request: CreateTransactionRequestDto): List<LogEventDto> {
        val nftTransactionClient = indexerApiClientFactory.getNftTransactionApiClient(blockchain)
        val orderTransactionClient = indexerApiClientFactory.getOrderTransactionApiClient(blockchain)

        return coroutineScope {
            val nftTransactions = async {
                nftTransactionClient.createNftPendingTransaction(request).collectList().awaitFirst()
            }
            val orderTransactions = async {
                orderTransactionClient.createOrderPendingTransaction(request).collectList().awaitFirst()
            }
            nftTransactions.await() + orderTransactions.await()
        }
    }
}