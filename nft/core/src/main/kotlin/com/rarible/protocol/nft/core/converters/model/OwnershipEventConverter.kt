package com.rarible.protocol.nft.core.converters.model

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.service.item.reduce.ItemUpdateService
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class OwnershipEventConverter(
    private val itemUpdateService: ItemUpdateService
) {
    suspend fun convert(source: ReversedEthereumLogRecord): List<OwnershipEvent> {
        return when (val data = source.data as? ItemHistory) {
            is ItemTransfer -> {
                val transferTo = data.owner.takeUnless { data.owner == Address.ZERO() }?.let { owner ->
                    OwnershipEvent.TransferToEvent(
                        from = data.from,
                        value = data.value,
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex,
                        minorLogIndex = source.minorLogIndex,
                        status = BlockchainStatusConverter.convert(source.status),
                        transactionHash = source.transactionHash.toString(),
                        address = source.address.prefixed(),
                        timestamp = source.createdAt.epochSecond,
                        entityId = OwnershipId(data.token, data.tokenId, owner).stringValue
                    )
                }
                val transferFrom = data.from.takeUnless { data.from == Address.ZERO() }?.let { from ->
                    OwnershipEvent.TransferFromEvent(
                        to = data.owner,
                        value = data.value,
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex,
                        minorLogIndex = source.minorLogIndex,
                        status = BlockchainStatusConverter.convert(source.status),
                        transactionHash = source.transactionHash.toString(),
                        address = source.address.prefixed(),
                        timestamp = source.createdAt.epochSecond,
                        entityId = OwnershipId(data.token, data.tokenId, from).stringValue
                    )
                }
                //Revertable event for lazy ownership to change value and lazyValue
                //Normally it is minting event, so 'from' must be ZERO address
                val changeLazyOwnership = data.from.takeIf { data.from == Address.ZERO() }?.let {
                    //Get lazy owner from item, but we should skip it if minter is lazy item owner
                    getItem(data.token, data.tokenId)?.getLazyOwner().takeUnless { it == data.owner }
                }?.let { lazyOwner ->
                    OwnershipEvent.ChangeLazyValueEvent(
                        value = data.value,
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex,
                        minorLogIndex = source.minorLogIndex,
                        status = BlockchainStatusConverter.convert(source.status),
                        transactionHash = source.transactionHash.toString(),
                        address = source.address.prefixed(),
                        timestamp = source.createdAt.epochSecond,
                        entityId = OwnershipId(data.token, data.tokenId, lazyOwner).stringValue
                    )
                }
                listOfNotNull(transferTo, transferFrom, changeLazyOwnership)
            }
            is ItemLazyMint -> {
                val lazyTransferTo = OwnershipEvent.LazyTransferToEvent(
                    value = data.value,
                    blockNumber = source.blockNumber,
                    logIndex = source.logIndex,
                    minorLogIndex = source.minorLogIndex,
                    status = BlockchainStatusConverter.convert(source.status),
                    transactionHash = source.transactionHash.toString(),
                    address = source.address.prefixed(),
                    timestamp = source.createdAt.epochSecond,
                    entityId = OwnershipId(data.token, data.tokenId, data.owner).stringValue
                )
                listOf(lazyTransferTo)
            }
            is ItemCreators,
            is ItemRoyalty,
            is BurnItemLazyMint,
            null -> emptyList()
        }
    }

    suspend fun convert(source: LogEvent): List<OwnershipEvent> {
        return convert(LogEventToReversedEthereumLogRecordConverter.convert(source))
    }

    private suspend fun getItem(token: Address, tokenId: EthUInt256): Item? {
        return itemUpdateService.get(ItemId(token, tokenId))
    }
}

