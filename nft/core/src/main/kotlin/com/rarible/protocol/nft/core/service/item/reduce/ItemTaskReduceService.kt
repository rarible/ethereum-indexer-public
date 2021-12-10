package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.core.entity.reducer.service.TaskReduceService
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import org.springframework.stereotype.Component

@Component
class ItemTaskReduceService(
    entityService: ItemUpdateService,
    entityEventService: ItemEventService,
    templateProvider: ItemTemplateProvider,
    reducer: ItemReducer
) : TaskReduceService<ItemId, ItemEvent, Item>(
    entityService = entityService,
    entityEventService = entityEventService,
    templateProvider = templateProvider,
    reducer = reducer
)
