package com.rarible.protocol.nft.core.converters.model

import com.rarible.protocol.nft.core.model.ItemEvent

object ItemEventInverter {
    fun invert(event: ItemEvent.ItemMintEvent): ItemEvent.ItemBurnEvent {
        return ItemEvent.ItemBurnEvent(
            supply = event.supply,
            owner = event.owner,
            entityId = event.entityId,
            log = event.log
        )
    }

    fun invert(event: ItemEvent.ItemBurnEvent): ItemEvent.ItemMintEvent {
        return ItemEvent.ItemMintEvent(
            supply = event.supply,
            owner = event.owner,
            entityId = event.entityId,
            log = event.log
        )
    }

    fun invert(event: ItemEvent.ItemTransferEvent): ItemEvent.ItemTransferEvent {
        return event.copy(from = event.to, to = event.from)
    }
}
