package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.item.reduce.forward.AbstractOpenSeaLazyValueItemReducer
import org.springframework.stereotype.Component

@Component
class ReversedOpenSeaLazyValueItemReducer : AbstractOpenSeaLazyValueItemReducer() {

    override suspend fun reduceItemTransferEvent(entity: Item, event: ItemEvent.OpenSeaLazyItemMintEvent): Item {
        return if (getTokenCreator(entity.tokenId) == event.from) {
            entity.copy(supply = entity.supply - event.supply)
        } else {
            entity
        }
    }
}
