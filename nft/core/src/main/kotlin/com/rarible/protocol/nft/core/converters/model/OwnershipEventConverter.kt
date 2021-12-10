package com.rarible.protocol.nft.core.converters.model

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.model.*
import scalether.domain.Address

object OwnershipEventConverter {
    fun convert(source: ReversedEthereumLogRecord): List<OwnershipEvent> {
        return when (val data = source.data as? ItemHistory) {
            is ItemTransfer -> {
                listOfNotNull(
                    data.owner
                        .takeUnless { data.owner == Address.ZERO() }
                        ?.let {
                            OwnershipEvent.TransferToEvent(
                                value = data.value,
                                blockNumber = source.blockNumber ?: error("Can't be null"),
                                logIndex = source.logIndex ?: error("Can't be null"),
                                status = BlockchainStatusConverter.convert(source.status),
                                minorLogIndex = source.minorLogIndex,
                                transactionHash = source.transactionHash,
                                timestamp = source.createdAt.epochSecond,
                                entityId = OwnershipId(data.token, data.tokenId, data.owner).stringValue
                            )
                        },
                    data.from
                        .takeUnless { data.from == Address.ZERO() }
                        ?.let {
                            OwnershipEvent.TransferFromEvent(
                                value = data.value,
                                blockNumber = source.blockNumber ?: error("Can't be null"),
                                logIndex = source.logIndex ?: error("Can't be null"),
                                status = BlockchainStatusConverter.convert(source.status),
                                transactionHash = source.transactionHash,
                                minorLogIndex = source.minorLogIndex,
                                timestamp = source.createdAt.epochSecond,
                                entityId = OwnershipId(data.token, data.tokenId, data.from).stringValue
                            )
                        }
                )
            }
            is ItemCreators,
            is ItemRoyalty,
            is BurnItemLazyMint,
            is ItemLazyMint,
            null -> emptyList()
        }
    }

    fun convert(source: LogEvent): List<OwnershipEvent> {
        return when (val data = source.data as? ItemHistory) {
            is ItemTransfer -> {
                listOfNotNull(
                    data.owner
                        .takeUnless { data.owner == Address.ZERO() }
                        ?.let {
                            OwnershipEvent.TransferToEvent(
                                value = data.value,
                                blockNumber = source.blockNumber ?: error("Can't be null"),
                                logIndex = source.logIndex ?: error("Can't be null"),
                                status = BlockchainStatusConverter.convert(source.status),
                                minorLogIndex = source.minorLogIndex,
                                transactionHash = source.transactionHash.toString(),
                                timestamp = source.createdAt.epochSecond,
                                entityId = OwnershipId(data.token, data.tokenId, data.owner).stringValue
                            )
                        },
                    data.from
                        .takeUnless { data.from == Address.ZERO() }
                        ?.let {
                            OwnershipEvent.TransferFromEvent(
                                value = data.value,
                                blockNumber = source.blockNumber ?: error("Can't be null"),
                                logIndex = source.logIndex ?: error("Can't be null"),
                                status = BlockchainStatusConverter.convert(source.status),
                                transactionHash = source.transactionHash.toString(),
                                minorLogIndex = source.minorLogIndex,
                                timestamp = source.createdAt.epochSecond,
                                entityId = OwnershipId(data.token, data.tokenId, data.from).stringValue
                            )
                        }
                )
            }
            is ItemCreators,
            is ItemRoyalty,
            is BurnItemLazyMint,
            is ItemLazyMint,
            null -> emptyList()
        }
    }
}
