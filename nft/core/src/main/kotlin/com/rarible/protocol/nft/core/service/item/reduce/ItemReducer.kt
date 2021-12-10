package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.core.entity.reducer.service.RevertableEntityReducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.EntityEventRevertService
import org.springframework.stereotype.Component

@Component
class ItemReducer(
    private val lazyItemReducer: LazyItemReducer,
    blockchainItemReducer: BlockchainItemReducer,
    entityEventRevertService: EntityEventRevertService<ItemEvent>
) : Reducer<ItemEvent, Item> {
    private val blockchainItemReducer = RevertableEntityReducer(entityEventRevertService, blockchainItemReducer)

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemBurnEvent,
            is ItemEvent.ItemMintEvent,
            is ItemEvent.ItemCreatorsEvent -> {
                blockchainItemReducer.reduce(entity, event)
            }
            is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent -> {
                lazyItemReducer.reduce(entity, event)
            }
        }
    }
}

