package com.rarible.protocol.nft.core.converters.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
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
                LogEventStatus.CONFIRMED -> EthereumLogStatus.CONFIRMED
                LogEventStatus.PENDING -> EthereumLogStatus.PENDING
                LogEventStatus.REVERTED -> EthereumLogStatus.REVERTED
                LogEventStatus.DROPPED -> EthereumLogStatus.DROPPED
                LogEventStatus.INACTIVE -> EthereumLogStatus.INACTIVE
            },
            topic = source.topic,
            minorLogIndex = source.minorLogIndex,
            index = source.index,
            address = source.address,
            blockHash = source.blockHash,
            blockNumber = source.blockNumber,
            logIndex = source.logIndex,
            visible = source.visible,
            createdAt = source.createdAt,
            updatedAt = source.updatedAt,
            data = source.data as EventData
        )
    }
}
