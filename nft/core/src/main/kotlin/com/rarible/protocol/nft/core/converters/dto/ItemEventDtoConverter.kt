package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.nft.core.model.Item
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.*

@Component
class ItemEventDtoConverter(
    private val itemDtoConverter: ItemDtoConverter
) : Converter<Item, NftItemEventDto> {
    override fun convert(item: Item): NftItemEventDto {
        val eventId = UUID.randomUUID().toString()

        return if (item.deleted) {
            NftItemDeleteEventDto(
                eventId = eventId,
                itemId = item.id.decimalStringValue,
                item = DeletedItemDtoConverter.convert(item.id)
            )
        } else {
            NftItemUpdateEventDto(
                eventId = eventId,
                itemId = item.id.decimalStringValue,
                item = itemDtoConverter.convert(item)
            )
        }
    }
}
