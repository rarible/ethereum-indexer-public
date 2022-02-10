package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.item.reduce.forward.AbstractOpenSeaLazyValueItemReducer
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ReversedOpenSeaLazyValueItemReducer(
    nftIndexerProperties: NftIndexerProperties
) : AbstractOpenSeaLazyValueItemReducer(Address.apply(nftIndexerProperties.openseaLazyMintAddress)) {

    override suspend fun reduceItemTransferEvent(entity: Item, event: ItemEvent.ItemTransferEvent): Item {
        return if (getTokenCreator(entity.tokenId) == event.from) {
            entity.copy(supply = entity.supply - event.value)
        } else {
            entity
        }
    }
}