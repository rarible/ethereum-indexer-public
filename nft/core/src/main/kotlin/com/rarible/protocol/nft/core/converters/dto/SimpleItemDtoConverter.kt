package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.nft.core.model.Item
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object SimpleItemDtoConverter : Converter<Item, NftItemDto> {
    override fun convert(source: Item): NftItemDto {
        return ItemDtoConverter.convert(source, null)
    }
}