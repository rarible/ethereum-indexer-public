package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.Part
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ForwardOpenSeaLazyValueItemReducer(
    nftIndexerProperties: NftIndexerProperties
) : AbstractOpenSeaLazyValueItemReducer(Address.apply(nftIndexerProperties.openseaLazyMintAddress)) {

    override fun reduceItemTransferEvent(entity: Item, event: ItemEvent.ItemTransferEvent): Item {
        val creator = getTokenCreator(entity.tokenId)
        return if (creator == event.from) {
            entity.copy(
                supply = entity.supply + event.value,
                creators = listOf(Part.fullPart(creator))
            )
        } else {
            entity
        }
    }
}

