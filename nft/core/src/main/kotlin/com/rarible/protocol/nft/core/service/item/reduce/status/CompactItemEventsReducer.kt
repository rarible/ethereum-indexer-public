package com.rarible.protocol.nft.core.service.item.reduce.status

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.AbstractCompactEventsReducer
import org.springframework.stereotype.Component

@Component
class CompactItemEventsReducer(
    featureFlags: FeatureFlags,
    properties: NftIndexerProperties.ReduceProperties,
) : AbstractCompactEventsReducer<ItemId, ItemEvent, Item>(featureFlags, properties) {

    override fun compact(events: List<ItemEvent>): List<ItemEvent> {
        return when (val last = events.last()) {
            is ItemEvent.ItemSupplyEvent -> {
                val supply = events.filterIsInstance<ItemEvent.ItemSupplyEvent>().sumOf { it.supply.value }
                listOf(last.withSupply(EthUInt256.of(supply)))
            }
            is ItemEvent.ItemCreatorsEvent,
            is ItemEvent.ItemTransferEvent,
            is ItemEvent.LazyItemBurnEvent,
            is ItemEvent.LazyItemMintEvent,
            is ItemEvent.OpenSeaLazyItemMintEvent -> events
        }
    }
}

