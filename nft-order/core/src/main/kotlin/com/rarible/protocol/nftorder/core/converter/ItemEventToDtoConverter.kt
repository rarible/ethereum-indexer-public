package com.rarible.protocol.nftorder.core.converter

import com.rarible.protocol.dto.NftOrderDeletedItemDto
import com.rarible.protocol.dto.NftOrderItemDeleteEventDto
import com.rarible.protocol.dto.NftOrderItemEventDto
import com.rarible.protocol.dto.NftOrderItemUpdateEventDto
import com.rarible.protocol.nftorder.core.event.ItemEvent
import com.rarible.protocol.nftorder.core.event.ItemEventDelete
import com.rarible.protocol.nftorder.core.event.ItemEventUpdate

object ItemEventToDtoConverter {

    fun convert(source: ItemEvent): NftOrderItemEventDto {
        return when (source) {
            is ItemEventUpdate -> NftOrderItemUpdateEventDto(
                eventId = source.id,
                itemId = source.item.id,
                item = source.item
            )
            is ItemEventDelete -> {
                NftOrderItemDeleteEventDto(
                    eventId = source.id,
                    itemId = source.itemId.decimalStringValue,
                    item = NftOrderDeletedItemDto(
                        id = source.itemId.stringValue,
                        token = source.itemId.token,
                        tokenId = source.itemId.tokenId.value
                    )
                )
            }
        }
    }
}
