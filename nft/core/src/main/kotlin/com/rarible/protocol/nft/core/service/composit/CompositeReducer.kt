package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.CompositeEvent
import com.rarible.protocol.nft.core.model.CompositeEntity
import com.rarible.protocol.nft.core.service.item.reduce.ItemReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.OwnershipReducer
import org.springframework.stereotype.Component

@Component
class CompositeReducer(
    private val itemReducer: ItemReducer,
    private val ownershipReducer: OwnershipReducer
) : Reducer<CompositeEvent, CompositeEntity> {

    override suspend fun reduce(entity: CompositeEntity, event: CompositeEvent): CompositeEntity {
        val reducedItem = entity.item?.let { item ->
            itemReducer.reduce(item, requireNotNull(event.itemEvent))
        }
        val ownerships = entity.ownerships.map { ownership ->
            ownershipReducer.reduce(ownership, event.ownershipEvents.single { it.entityId == ownership.id.stringValue })
        }
        return CompositeEntity(reducedItem, ownerships)
    }
}
