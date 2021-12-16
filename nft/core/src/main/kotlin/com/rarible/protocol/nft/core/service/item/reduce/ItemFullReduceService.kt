package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.core.entity.reducer.service.StreamFullReduceService
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import org.springframework.stereotype.Component

@Component
class ItemFullReduceService(
    entityService: ItemUpdateService,
    entityIdService: ItemIdService,
    templateProvider: ItemTemplateProvider,
    reducer: ItemReducer
) : StreamFullReduceService<ItemId, ItemEvent, Item>(
    entityService = entityService,
    entityEventService = entityIdService,
    templateProvider = templateProvider,
    reducer = reducer
)
