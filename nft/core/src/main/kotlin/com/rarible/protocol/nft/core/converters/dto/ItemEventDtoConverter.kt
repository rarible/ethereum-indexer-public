package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.nft.core.misc.addOut
import com.rarible.protocol.nft.core.misc.nftOffchainEventMarks
import com.rarible.protocol.nft.core.misc.toDto
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import java.util.UUID

object ItemEventDtoConverter {

    fun convert(item: Item, event: ItemEvent?): NftItemEventDto {
        val eventId = UUID.randomUUID().toString()

        val marks = (event?.eventTimeMarks ?: nftOffchainEventMarks()).addOut().toDto()

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
