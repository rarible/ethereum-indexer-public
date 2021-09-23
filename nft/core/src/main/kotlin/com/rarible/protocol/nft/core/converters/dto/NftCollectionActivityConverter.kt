package com.rarible.protocol.nft.core.converters.dto

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.dto.NftCollectionActivityDto
import com.rarible.protocol.dto.NftCollectionCreatedDto
import com.rarible.protocol.dto.NftCollectionOwnershipTransferredDto
import com.rarible.protocol.nft.core.model.CollectionEvent
import com.rarible.protocol.nft.core.model.CollectionOwnershipTransferred
import com.rarible.protocol.nft.core.model.CreateCollection
import io.daonomic.rpc.domain.Word

object NftCollectionActivityConverter {
    fun convert(logEvent: LogEvent): NftCollectionActivityDto? =
        when (val eventData = logEvent.data as? CollectionEvent) {
            null -> null
            is CreateCollection -> NftCollectionCreatedDto(
                blockHash = logEvent.blockHash ?: DEFAULT_BLOCK_HASH,
                blockNumber = logEvent.blockNumber ?: DEFAULT_BLOCK_NUMBER,
                transactionHash = logEvent.transactionHash,
                logIndex = logEvent.logIndex ?: DEFAULT_LOG_INDEX,
                id = logEvent.id.toString(),

                contract = eventData.id,
                date = eventData.date,
                owner = eventData.owner,
                name = eventData.name,
                symbol = eventData.symbol
            )
            is CollectionOwnershipTransferred -> NftCollectionOwnershipTransferredDto(
                blockHash = logEvent.blockHash ?: DEFAULT_BLOCK_HASH,
                blockNumber = logEvent.blockNumber ?: DEFAULT_BLOCK_NUMBER,
                transactionHash = logEvent.transactionHash,
                logIndex = logEvent.logIndex ?: DEFAULT_LOG_INDEX,
                id = logEvent.id.toString(),

                contract = eventData.id,
                date = eventData.date,
                previousOwner = eventData.previousOwner,
                newOwner = eventData.newOwner
            )
        }

    private val DEFAULT_BLOCK_HASH: Word = Word.apply(ByteArray(32))
    private const val DEFAULT_BLOCK_NUMBER: Long = 0
    private const val DEFAULT_LOG_INDEX: Int = 0
}
