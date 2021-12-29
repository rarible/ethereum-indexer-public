package com.rarible.protocol.nft.api.service.pending

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.log.domain.TransactionDto
import com.rarible.protocol.nft.core.converters.model.LogEventToReversedEthereumLogRecordConverter
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.ReduceVersion
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class FeaturedPendingTransactionService(
    private val legacyPendingTransactionService: LegacyPendingTransactionService,
    private val pendingTransactionService: PendingTransactionServiceImp,
    private val featureFlags: FeatureFlags
) : PendingTransactionService {

    override suspend fun process(tx: TransactionDto): List<ReversedEthereumLogRecord> {
        return when (featureFlags.reduceVersion) {
            ReduceVersion.V1 -> legacyPendingTransactionService.process(tx).map {
                LogEventToReversedEthereumLogRecordConverter.convert(it)
            }
            ReduceVersion.V2 -> pendingTransactionService.process(tx)
        }
    }
}

