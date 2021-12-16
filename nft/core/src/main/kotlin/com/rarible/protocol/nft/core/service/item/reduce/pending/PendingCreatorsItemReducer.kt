package com.rarible.protocol.nft.core.service.item.reduce.pending

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.item.reduce.forward.ForwardCreatorsItemReducer
import org.springframework.stereotype.Component

@Component
class PendingCreatorsItemReducer(
    private val delegate: ForwardCreatorsItemReducer
) : Reducer<ItemEvent, Item> {

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return if (entity.creators.isEmpty()) delegate.reduce(entity, event) else entity
    }
}
