package com.rarible.protocol.nft.core.converters.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.EventData
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus

object LogEventToReversedEthereumLogRecordConverter {
    fun convert(source: LogEvent): ReversedEthereumLogRecord {
        return ReversedEthereumLogRecord(
            id = source.id.toHexString(),
            version = source.version,
            transactionHash = source.transactionHash.prefixed(),
            status = when (source.status) {
                LogEventStatus.CONFIRMED -> EthereumBlockStatus.CONFIRMED
                LogEventStatus.PENDING -> EthereumBlockStatus.PENDING
                LogEventStatus.REVERTED -> EthereumBlockStatus.REVERTED
                LogEventStatus.DROPPED -> EthereumBlockStatus.DROPPED
                LogEventStatus.INACTIVE -> EthereumBlockStatus.INACTIVE
            },
            topic = source.topic,
            minorLogIndex = source.minorLogIndex,
            index = source.index,
            address = source.address,
            blockHash = source.blockHash,
            blockNumber = source.blockNumber,
            from = source.from,
            blockTimestamp = source.blockTimestamp,
            logIndex = source.logIndex,
            visible = source.visible,
            createdAt = source.createdAt,
            updatedAt = source.updatedAt,
            data = source.data as EventData
        )
    }
}
