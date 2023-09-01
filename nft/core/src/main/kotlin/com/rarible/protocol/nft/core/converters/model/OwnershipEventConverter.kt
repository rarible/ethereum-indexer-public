package com.rarible.protocol.nft.core.converters.model

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.common.EventTimeMarks
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

@Component
class OwnershipEventConverter(
    private val itemUpdateService: ItemUpdateService
) {

    suspend fun convert(
        events: List<Pair<ReversedEthereumLogRecord, EventTimeMarks>>,
    ): List<OwnershipEvent> {
        val mintTransfersItemIds = events.mapNotNull { pair ->
            val source = pair.first
            val data = source.data as? ItemTransfer ?: return@mapNotNull null
            ItemId(data.token, data.tokenId)
        }
        val items = itemUpdateService.getAll(mintTransfersItemIds).associateBy { it.id }
        return events.flatMap { convert(it.first, it.second, items) }
    }

    suspend fun convert(
        source: ReversedEthereumLogRecord,
        eventTimeMarks: EventTimeMarks? = null,
        itemHint: Map<ItemId, Item> = emptyMap()
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
                // Revertable event for lazy ownership to change value and lazyValue
                // Normally it is minting event
                val changeLazyOwnership = data.from.takeIf { data.isMintTransfer() }?.let {
                    // Get lazy owner from item, but we should skip it if minter is lazy item owner
                    val itemId = ItemId(data.token, data.tokenId)
                    (itemHint[itemId] ?: getItem(itemId))?.getLazyOwner().takeUnless { it == data.owner }
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

    private suspend fun getItem(itemId: ItemId): Item? {
        return itemUpdateService.get(itemId)
    }
}
