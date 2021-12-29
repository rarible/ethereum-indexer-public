package com.rarible.protocol.nft.api.service.pending

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.log.domain.TransactionDto

interface PendingTransactionService {
    suspend fun process(tx: TransactionDto): List<ReversedEthereumLogRecord>
}