package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.CompositeEvent
import com.rarible.protocol.nft.core.model.CompositeEntity
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.service.item.reduce.ItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.ItemTemplateProvider
import com.rarible.protocol.nft.core.service.ownership.reduce.OwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.OwnershipTemplateProvider
import org.springframework.stereotype.Component

@Component
class CompositeReducer(
    private val itemReducer: ItemReducer,
    private val ownershipReducer: OwnershipReducer,
    private val ownershipTemplateProvider: OwnershipTemplateProvider,
    private val itemTemplateProvider: ItemTemplateProvider
) : Reducer<CompositeEvent, CompositeEntity> {

    override suspend fun reduce(entity: CompositeEntity, event: CompositeEvent): CompositeEntity {
        val reducedItem = event.itemEvent?.let { itemEvent ->
            val item = entity.item ?: itemTemplateProvider.getEntityTemplate(entity.id)
            itemReducer.reduce(item, itemEvent)
        }

        val reducedOwnerships = entity.ownerships.associateBy { it.id }.toMutableMap()

        event.ownershipEvents.forEach { ownershipEvent ->
            val eventOwnershipId = OwnershipId.parseId(ownershipEvent.entityId)
            val ownership = reducedOwnerships[eventOwnershipId] ?: ownershipTemplateProvider.getEntityTemplate(eventOwnershipId)
            reducedOwnerships[eventOwnershipId] = ownershipReducer.reduce(ownership, ownershipEvent)
        }
        return CompositeEntity(
            id = entity.id,
            item = reducedItem ?: entity.item,
            ownerships = reducedOwnerships.values.toList()
        )
    }
}
