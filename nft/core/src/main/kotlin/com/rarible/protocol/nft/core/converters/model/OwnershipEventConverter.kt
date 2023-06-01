package com.rarible.protocol.nft.core.converters.model

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.common.EventTimeMarks
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.model.BurnItemLazyMint
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemCreators
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.ItemRoyalty
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.service.item.reduce.ItemUpdateService
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class OwnershipEventConverter(
    private val itemUpdateService: ItemUpdateService
) {

    suspend fun convert(
        source: ReversedEthereumLogRecord,
        eventTimeMarks: EventTimeMarks? = null
    ): List<OwnershipEvent> {
        val events = when (val data = source.data as? ItemHistory) {
            is ItemTransfer -> {
                val transferTo = data.owner.takeUnless { data.isBurnTransfer() }?.let { owner ->
                    OwnershipEvent.TransferToEvent(
                        from = data.from,
                        value = data.value,
                        log = source.log,
                        entityId = OwnershipId(data.token, data.tokenId, owner).stringValue,
                    )
                }
                val transferFrom = data.from.takeUnless { data.isMintTransfer() }?.let { from ->
                    OwnershipEvent.TransferFromEvent(
                        to = data.owner,
                        value = data.value,
                        log = source.log,
                        entityId = OwnershipId(data.token, data.tokenId, from).stringValue,
                    )
                }
                //Revertable event for lazy ownership to change value and lazyValue
                //Normally it is minting event
                val changeLazyOwnership = data.from.takeIf { data.isMintTransfer() }?.let {
                    //Get lazy owner from item, but we should skip it if minter is lazy item owner
                    getItem(data.token, data.tokenId)?.getLazyOwner().takeUnless { it == data.owner }
                }?.let { lazyOwner ->
                    OwnershipEvent.ChangeLazyValueEvent(
                        value = data.value,
                        log = source.log,
                        entityId = OwnershipId(data.token, data.tokenId, lazyOwner).stringValue,
                    )
                }
                listOfNotNull(transferTo, transferFrom, changeLazyOwnership)
            }
            is ItemLazyMint -> {
                val lazyTransferTo = OwnershipEvent.LazyTransferToEvent(
                    value = data.value,
                    log = source.log,
                    entityId = OwnershipId(data.token, data.tokenId, data.owner).stringValue,
                )
                listOf(lazyTransferTo)
            }
            is BurnItemLazyMint -> {
                val lazyBurnEvent = OwnershipEvent.LazyBurnEvent(
                    from = data.from,
                    value = data.value,
                    log = source.log,
                    entityId = OwnershipId(data.token, data.tokenId, data.from).stringValue,
                )
                listOf(lazyBurnEvent)
            }

            is ItemCreators,
            is ItemRoyalty,
            null -> emptyList()
        }
        events.forEach { it.eventTimeMarks = eventTimeMarks }
        return events
    }

    suspend fun convert(source: LogEvent, eventTimeMarks: EventTimeMarks? = null): List<OwnershipEvent> {
        return convert(LogEventToReversedEthereumLogRecordConverter.convert(source), eventTimeMarks)
    }

    private suspend fun getItem(token: Address, tokenId: EthUInt256): Item? {
        return itemUpdateService.get(ItemId(token, tokenId))
    }
}
