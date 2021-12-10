package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.entity.reducer.service.EntityEventService
import com.rarible.protocol.nft.core.model.CompositeEntityId
import com.rarible.protocol.nft.core.model.CompositeEvent
import com.rarible.protocol.nft.core.service.item.reduce.ItemEventService
import com.rarible.protocol.nft.core.service.ownership.reduce.OwnershipEventService
import org.springframework.stereotype.Component

@Component
class CompositeEventService(
    private val itemEventService: ItemEventService,
    private val ownershipEventService: OwnershipEventService
) : EntityEventService<CompositeEvent, CompositeEntityId> {

    override fun getEntityId(event: CompositeEvent): CompositeEntityId {
        return CompositeEntityId(
            itemId = event.itemEvent?.let { itemEventService.getEntityId(it) },
            ownershipIds = event.ownershipEvents.map { ownershipEventService.getEntityId(it) }
        )
    }
}
