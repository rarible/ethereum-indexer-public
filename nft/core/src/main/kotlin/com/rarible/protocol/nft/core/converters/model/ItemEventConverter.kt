package com.rarible.protocol.nft.core.converters.model

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.model.*
import scalether.domain.Address

object ItemEventConverter {
    fun convert(source: ReversedEthereumLogRecord): ItemEvent? {
        return when (val data = source.data as? ItemHistory) {
            is ItemTransfer -> {
                when {
                    data.from == Address.ZERO() -> ItemEvent.ItemMintEvent(
                        supply = data.value,
                        owner = data.owner,
                        blockNumber = source.blockNumber ?: error("Can't be null"),
                        logIndex = source.logIndex ?: error("Can't be null"),
                        status = BlockchainStatusConverter.convert(source.status),
                        minorLogIndex = source.minorLogIndex,
                        transactionHash = source.transactionHash,
                        timestamp = source.createdAt.epochSecond,
                        entityId = ItemId(data.token, data.tokenId).stringValue
                    )
                    data.owner == Address.ZERO() -> ItemEvent.ItemBurnEvent(
                        supply = data.value,
                        blockNumber = source.blockNumber ?: error("Can't be null"),
                        logIndex = source.logIndex ?: error("Can't be null"),
                        status = BlockchainStatusConverter.convert(source.status),
                        transactionHash = source.transactionHash,
                        minorLogIndex = source.minorLogIndex,
                        timestamp = source.createdAt.epochSecond,
                        entityId = ItemId(data.token, data.tokenId).stringValue
                    )
                    else -> null
                }
            }
            is ItemLazyMint -> {
                ItemEvent.LazyItemMintEvent(
                    supply = data.value,
                    blockNumber = source.blockNumber ?: error("Can't be null"),
                    logIndex = source.logIndex ?: error("Can't be null"),
                    status = BlockchainStatusConverter.convert(source.status),
                    transactionHash = source.transactionHash,
                    minorLogIndex = source.minorLogIndex,
                    timestamp = source.createdAt.epochSecond,
                    entityId = ItemId(data.token, data.tokenId).stringValue
                )
            }
            is BurnItemLazyMint -> {
                ItemEvent.LazyItemBurnEvent(
                    supply = data.value,
                    blockNumber = source.blockNumber ?: error("Can't be null"),
                    logIndex = source.logIndex ?: error("Can't be null"),
                    status = BlockchainStatusConverter.convert(source.status),
                    transactionHash = source.transactionHash,
                    minorLogIndex = source.minorLogIndex,
                    timestamp = source.createdAt.epochSecond,
                    entityId = ItemId(data.token, data.tokenId).stringValue
                )
            }
            is ItemCreators, is ItemRoyalty, null -> null
        }
    }

    fun convert(source: LogEvent): ItemEvent? {
        return when (val data = source.data as? ItemHistory) {
            is ItemTransfer -> {
                when {
                    data.from == Address.ZERO() -> ItemEvent.ItemMintEvent(
                        supply = data.value,
                        owner = data.owner,
                        blockNumber = source.blockNumber ?: error("Can't be null"),
                        logIndex = source.logIndex ?: error("Can't be null"),
                        status = BlockchainStatusConverter.convert(source.status),
                        minorLogIndex = source.minorLogIndex,
                        transactionHash = source.transactionHash.toString(),
                        timestamp = source.createdAt.epochSecond,
                        entityId = ItemId(data.token, data.tokenId).stringValue
                    )
                    data.owner == Address.ZERO() -> ItemEvent.ItemBurnEvent(
                        supply = data.value,
                        blockNumber = source.blockNumber ?: error("Can't be null"),
                        logIndex = source.logIndex ?: error("Can't be null"),
                        status = BlockchainStatusConverter.convert(source.status),
                        transactionHash = source.transactionHash.toString(),
                        minorLogIndex = source.minorLogIndex,
                        timestamp = source.createdAt.epochSecond,
                        entityId = ItemId(data.token, data.tokenId).stringValue
                    )
                    else -> null
                }
            }
            is ItemLazyMint -> {
                ItemEvent.LazyItemMintEvent(
                    supply = data.value,
                    blockNumber = source.blockNumber ?: error("Can't be null"),
                    logIndex = source.logIndex ?: error("Can't be null"),
                    status = BlockchainStatusConverter.convert(source.status),
                    transactionHash = source.transactionHash.toString(),
                    minorLogIndex = source.minorLogIndex,
                    timestamp = source.createdAt.epochSecond,
                    entityId = ItemId(data.token, data.tokenId).stringValue
                )
            }
            is BurnItemLazyMint -> {
                ItemEvent.LazyItemBurnEvent(
                    supply = data.value,
                    blockNumber = source.blockNumber ?: error("Can't be null"),
                    logIndex = source.logIndex ?: error("Can't be null"),
                    status = BlockchainStatusConverter.convert(source.status),
                    transactionHash = source.transactionHash.toString(),
                    minorLogIndex = source.minorLogIndex,
                    timestamp = source.createdAt.epochSecond,
                    entityId = ItemId(data.token, data.tokenId).stringValue
                )
            }
            is ItemCreators, is ItemRoyalty, null -> null
        }
    }
}

