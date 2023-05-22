package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.dto.blockchainEventMark
import com.rarible.protocol.dto.offchainEventMark
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import java.util.UUID

object ItemEventDtoConverter {

    fun convert(item: Item, event: ItemEvent?): NftItemEventDto {
        val eventId = UUID.randomUUID().toString()

        val markName = "indexer-out_nft"
        val marks = event?.eventTimeMarks?.addOut("nft")?.toDto()
            ?: event?.log?.blockTimestamp?.let { blockchainEventMark(markName, it) }
            ?: offchainEventMark(markName)

        return if (item.deleted) {
            NftItemDeleteEventDto(
                eventId = eventId,
                itemId = item.id.decimalStringValue,
                item = DeletedItemDtoConverter.convert(item.id),
                eventTimeMarks = marks
            )
        } else {
            NftItemUpdateEventDto(
                eventId = eventId,
                itemId = item.id.decimalStringValue,
                item = ItemDtoConverter.convert(item),
                eventTimeMarks = marks
            )
        }
    }
}
