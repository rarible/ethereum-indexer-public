package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.nft.core.model.ExtendedItem
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object ExtendedItemDtoConverter : Converter<ExtendedItem, NftItemDto> {
    override fun convert(source: ExtendedItem): NftItemDto {
        return ItemDtoConverter.convert(source.item, source.itemMeta)
    }
}