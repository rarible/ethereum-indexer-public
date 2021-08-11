package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.nft.core.model.*
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.*

@Component
object ItemEventDtoConverter : Converter<Item, NftItemEventDto> {
    override fun convert(source: Item): NftItemEventDto {
        val eventId = UUID.randomUUID().toString()

        return if (source.deleted) {
            val itemId = source.id

            NftItemDeleteEventDto(
                eventId,
                itemId.decimalStringValue,
                DeletedItemDtoConverter.convert(itemId)
            )
        } else {
            NftItemUpdateEventDto(
                eventId,
                source.id.decimalStringValue,
                ItemDtoConverter.convert(source, null)
            )
        }
    }
}
