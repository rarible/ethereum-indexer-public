package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.Part
import org.springframework.stereotype.Component

@Component
class ForwardOpenSeaLazyValueItemReducer : AbstractOpenSeaLazyValueItemReducer() {
    override suspend fun reduceItemTransferEvent(entity: Item, event: ItemEvent.OpenSeaLazyItemMintEvent): Item {
        val creator = getTokenCreator(entity.tokenId)
        return if (creator == event.from) {
            entity.copy(
                supply = entity.supply + event.supply,
                creators = listOf(Part.fullPart(creator))
            )
        } else {
            entity
        }
    }
}
