package com.rarible.protocol.nft.core.converters.dto

import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.nft.core.misc.addIndexerOut
import com.rarible.protocol.nft.core.misc.toDto
import com.rarible.protocol.nft.core.model.Item
import java.util.UUID

object ItemEventDtoConverter {

    fun convert(item: Item, eventTimeMarks: EventTimeMarks): NftItemEventDto {
        val eventId = UUID.randomUUID().toString()

        val marks = eventTimeMarks.addIndexerOut().toDto()

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
