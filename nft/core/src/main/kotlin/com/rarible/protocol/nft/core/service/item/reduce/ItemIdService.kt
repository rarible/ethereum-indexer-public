package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.core.entity.reducer.service.EntityIdService
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import org.springframework.stereotype.Component

@Component
class ItemIdService : EntityIdService<ItemEvent, ItemId> {
    override fun getEntityId(event: ItemEvent): ItemId {
        return ItemId.parseId(event.entityId)
    }
}
