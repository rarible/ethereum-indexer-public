package com.rarible.protocol.nft.core.misc

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.core.model.LazyItemHistory
import io.daonomic.rpc.domain.Word
import org.bson.types.ObjectId
import scalether.domain.Address

private val WORD_ZERO: Word = Word.apply(ByteArray(32))

fun LazyItemHistory.wrapWithEthereumLogRecord(): ReversedEthereumLogRecord {
    val createdAt = nowMillis()
    return ReversedEthereumLogRecord(
        id = ObjectId().toHexString(),
        version = null,
        transactionHash = WORD_ZERO.prefixed(),
        status = EthereumLogStatus.CONFIRMED,
        topic = WORD_ZERO,
        blockNumber = -1,
        logIndex = -1,
        minorLogIndex = 0,
        index = 0,
        address = Address.ZERO(),
        blockHash = WORD_ZERO,
        from = Address.ZERO(),
        blockTimestamp = this.date.epochSecond,
        visible = true,
        createdAt = createdAt,
        updatedAt = createdAt,
        data = this
    )
}