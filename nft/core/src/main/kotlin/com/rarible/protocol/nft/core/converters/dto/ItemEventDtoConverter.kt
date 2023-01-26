package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.dto.blockchainEventMark
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import java.util.*

object ItemEventDtoConverter {

    fun convert(item: Item, event: ItemEvent?): NftItemEventDto {
        val eventId = UUID.randomUUID().toString()

        return if (item.deleted) {
            NftItemDeleteEventDto(
                eventId = eventId,
                itemId = item.id.decimalStringValue,
                item = DeletedItemDtoConverter.convert(item.id),
                eventTimeMarks = blockchainEventMark(event?.log?.blockTimestamp)
            )
        } else {
            NftItemUpdateEventDto(
                eventId = eventId,
                itemId = item.id.decimalStringValue,
                item = ItemDtoConverter.convert(item),
                eventTimeMarks = blockchainEventMark(event?.log?.blockTimestamp)
            )
        }
    }
}
