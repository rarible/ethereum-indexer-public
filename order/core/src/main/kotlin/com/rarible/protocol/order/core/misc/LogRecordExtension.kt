package com.rarible.protocol.order.core.misc

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.model.LogRecord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.model.EventData
import io.daonomic.rpc.domain.Word

fun LogRecord.asEthereumLogRecord() = this as ReversedEthereumLogRecord

fun LogRecord.toLogEvent(): LogEvent {
    return asEthereumLogRecord().let {  record ->
        LogEvent(
            data = record.data as EventData,
            address = record.address,
            topic = record.topic,
            transactionHash = Word.apply(record.transactionHash),
            status = record.status.toLogEventStatus(),
            blockHash = record.blockHash,
            blockNumber = record.blockNumber,
            logIndex = record.logIndex,
            from = record.from,
            to = record.to,
            blockTimestamp = record.blockTimestamp,
            minorLogIndex = record.minorLogIndex,
            index = record.index,
            visible = record.visible,
            version = record.version,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )
    }
}

fun LogEvent.toReversedEthereumLogRecord(): ReversedEthereumLogRecord {
    return ReversedEthereumLogRecord(
        id = this.id.toHexString(),
        version = this.version,
        transactionHash = this.transactionHash.prefixed(),
        status = this.status.toEthereumLogStatus(),
        topic = this.topic,
        minorLogIndex = this.minorLogIndex,
        index = this.index,
        address = this.address,
        blockHash = this.blockHash,
        blockNumber = this.blockNumber,
        logIndex = this.logIndex,
        blockTimestamp = this.blockTimestamp,
        from = this.from,
        to = this.to,
        visible = this.visible,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        data = this.data as EventData
    )
}

internal fun EthereumLogStatus.toLogEventStatus(): LogEventStatus {
    return when (this) {
        EthereumLogStatus.CONFIRMED -> LogEventStatus.CONFIRMED
        EthereumLogStatus.REVERTED -> LogEventStatus.REVERTED
        EthereumLogStatus.PENDING -> LogEventStatus.PENDING
        EthereumLogStatus.DROPPED -> LogEventStatus.DROPPED
        EthereumLogStatus.INACTIVE -> LogEventStatus.INACTIVE
    }
}

internal fun LogEventStatus.toEthereumLogStatus(): EthereumLogStatus {
    return when (this) {
        LogEventStatus.CONFIRMED -> EthereumLogStatus.CONFIRMED
        LogEventStatus.REVERTED -> EthereumLogStatus.REVERTED
        LogEventStatus.PENDING -> EthereumLogStatus.PENDING
        LogEventStatus.DROPPED -> EthereumLogStatus.DROPPED
        LogEventStatus.INACTIVE -> EthereumLogStatus.INACTIVE
    }
}
