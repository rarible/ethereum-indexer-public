package com.rarible.protocol.nft.core.misc

import com.rarible.protocol.nft.core.model.ItemEvent

fun ItemEvent.cleanEventTimeMarks(): ItemEvent {
    val event = when (this) {
        is ItemEvent.ItemBurnEvent -> this.copy()
        is ItemEvent.ItemCreatorsEvent -> this.copy()
        is ItemEvent.ItemMintEvent -> this.copy()
        is ItemEvent.ItemTransferEvent -> this.copy()
        is ItemEvent.LazyItemBurnEvent -> this.copy()
        is ItemEvent.LazyItemMintEvent -> this.copy()
        is ItemEvent.OpenSeaLazyItemMintEvent -> this.copy()
    }
    event.eventTimeMarks = null
    return event
}
