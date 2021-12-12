package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.entity.reducer.service.EntityIdService
import com.rarible.protocol.nft.core.model.CompositeEntityId
import com.rarible.protocol.nft.core.model.CompositeEvent
import com.rarible.protocol.nft.core.service.item.reduce.ItemIdService
import com.rarible.protocol.nft.core.service.ownership.reduce.OwnershipIdService
import org.springframework.stereotype.Component

@Component
class CompositeEventService(
    private val itemIdService: ItemIdService,
    private val ownershipIdService: OwnershipIdService
) : EntityIdService<CompositeEvent, CompositeEntityId> {

    override fun getEntityId(event: CompositeEvent): CompositeEntityId {
        return CompositeEntityId(
            itemId = event.itemEvent?.let { itemIdService.getEntityId(it) },
            ownershipIds = event.ownershipEvents.map { ownershipIdService.getEntityId(it) }
        )
    }
}
