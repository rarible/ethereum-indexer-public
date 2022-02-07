package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.ethereum.domain.EthUInt256
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

    override fun reduceItemTransferEvent(entity: Item, event: ItemEvent.ItemTransferEvent): Item {
        return if (getTokenCreator(entity.tokenId) == event.from) {
            val ownerships = entity.ownerships.toMutableMap()
            val ownerValue = ownerships[event.from] ?: EthUInt256.ZERO

            if (event.value > EthUInt256.ZERO && ownerValue > event.value) {
                ownerships[event.from] = ownerValue - event.value
            } else {
                ownerships.remove(event.from)
            }
            entity.copy(
                supply = entity.supply - event.value,
                ownerships = ownerships
            )
        } else {
            entity
        }
    }
}