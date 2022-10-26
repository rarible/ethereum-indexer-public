package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.blockchain.scanner.ethereum.reduce.AbstractMetricReducer
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class ItemMetricReducer(
    properties: NftIndexerProperties,
    meterRegistry: MeterRegistry,
) : AbstractMetricReducer<ItemEvent, Item>(properties, meterRegistry, "item") {

    override fun getMetricName(event: ItemEvent): String {
        return when (event) {
            is ItemEvent.ItemBurnEvent -> "burn"
            is ItemEvent.ItemCreatorsEvent -> "creators"
            is ItemEvent.ItemMintEvent -> "mint"
            is ItemEvent.ItemTransferEvent -> "transfer"
            is ItemEvent.LazyItemBurnEvent -> "lazy_burn"
            is ItemEvent.LazyItemMintEvent -> "lazy_mint"
            is ItemEvent.OpenSeaLazyItemMintEvent -> "open_sea_lazy_mint"
        }
    }
}
