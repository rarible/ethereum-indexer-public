package com.rarible.protocol.order.core.model

import com.rarible.ethereum.listener.log.domain.LogEvent
import io.daonomic.rpc.domain.Word
import scalether.domain.Address

/**
 * Key descriptor that uniquely identifies a log event [LogEvent] in the blockchain.
 * [databaseKey] may be used as a database ID for storing objects.
 */
data class LogEventKey(
    val blockNumber: Long?,
    val transactionHash: Word,
    val topic: Word,
    val address: Address,
    val index: Int,
    val minorLogIndex: Int,

    val databaseKey: String = "$blockNumber.${transactionHash.hex()}.${topic.hex()}.${address.hex()}.$index.$minorLogIndex"
) : Comparable<LogEventKey> {
    override fun compareTo(other: LogEventKey): Int {
        if ((blockNumber == null) xor (other.blockNumber == null)) {
            // Pending logs go last.
            return if (blockNumber == null) 1 else -1
        }
        if (blockNumber != null && other.blockNumber != null) {
            blockNumber.compareTo(other.blockNumber).takeUnless { it == 0 }?.let { return it }
        }
        index.compareTo(other.index).takeUnless { it == 0 }?.let { return it }
        return minorLogIndex.compareTo(other.minorLogIndex)
    }
}

fun LogEvent.toLogEventKey() = LogEventKey(
    blockNumber = blockNumber,
    transactionHash = transactionHash,
    topic = topic,
    address = address,
    index = index,
    minorLogIndex = minorLogIndex
)

