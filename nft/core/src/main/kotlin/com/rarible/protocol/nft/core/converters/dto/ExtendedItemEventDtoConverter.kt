package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.nft.core.model.ExtendedItem
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.*

@Component
class ExtendedItemEventDtoConverter(
    private val extendedItemDtoConverter: ExtendedItemDtoConverter
) : Converter<ExtendedItem, NftItemEventDto> {
    override fun convert(source: ExtendedItem): NftItemEventDto {
        val eventId = UUID.randomUUID().toString()

        return if (source.item.deleted) {
            val itemId = source.item.id

            NftItemDeleteEventDto(
                eventId,
                itemId.decimalStringValue,
                DeletedItemDtoConverter.convert(itemId)
            )
        } else {
            NftItemUpdateEventDto(
                eventId,
                source.item.id.decimalStringValue,
                extendedItemDtoConverter.convert(source)
            )
        }
    }
}
