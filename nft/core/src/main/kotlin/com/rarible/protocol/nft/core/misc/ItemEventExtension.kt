package com.rarible.protocol.nft.core.misc

import com.rarible.protocol.nft.core.model.ItemEvent

fun ItemEvent.cleanEventTimeMarks(): ItemEvent {
    return when (this) {
        is ItemEvent.ItemMintEvent -> this.copy(eventTimeMarks = null)
        is ItemEvent.ItemBurnEvent -> this.copy(eventTimeMarks = null)
        is ItemEvent.ItemTransferEvent -> this.copy(eventTimeMarks = null)
        is ItemEvent.ItemCreatorsEvent -> this.copy(eventTimeMarks = null)
        is ItemEvent.OpenSeaLazyItemMintEvent -> this.copy(eventTimeMarks = null)
        is ItemEvent.LazyItemBurnEvent -> this.copy(eventTimeMarks = null)
        is ItemEvent.LazyItemMintEvent -> this.copy(eventTimeMarks = null)
    }
}