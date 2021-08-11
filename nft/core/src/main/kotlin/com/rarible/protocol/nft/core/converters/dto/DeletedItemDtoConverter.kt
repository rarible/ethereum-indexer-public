package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftDeletedItemDto
import com.rarible.protocol.nft.core.model.ItemId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object DeletedItemDtoConverter : Converter<ItemId, NftDeletedItemDto> {
    override fun convert(source: ItemId): NftDeletedItemDto {
        return NftDeletedItemDto(
            id = source.decimalStringValue,
            token = source.token,
            tokenId = source.tokenId.value
        )
    }
}