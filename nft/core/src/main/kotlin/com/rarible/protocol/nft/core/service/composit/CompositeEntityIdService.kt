package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.entity.reducer.service.EntityIdService
import com.rarible.protocol.nft.core.model.CompositeEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.reduce.ItemIdService
import com.rarible.protocol.nft.core.service.ownership.reduce.OwnershipIdService
import org.springframework.stereotype.Component

@Component
class CompositeEntityIdService(
    private val itemIdService: ItemIdService,
    private val ownershipIdService: OwnershipIdService
) : EntityIdService<CompositeEvent, ItemId> {

    override fun getEntityId(event: CompositeEvent): ItemId {
        val itemId = event.itemEvent?.let { itemIdService.getEntityId(it) }
        val ownershipIds = event.ownershipEvents.map { ownershipIdService.getEntityId(it) }
        return when {
            itemId != null -> itemId
            ownershipIds.isNotEmpty() -> ownershipIds.first().let { ItemId(it.token, it.tokenId) }
            else -> throw IllegalArgumentException("Can't get itemId from event $event")
        }
    }
}
