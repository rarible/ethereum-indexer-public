package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.ItemRoyaltyDto
import com.rarible.protocol.nft.core.model.ItemRoyalty
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object ItemRoyaltyDtoConverter : Converter<ItemRoyalty, ItemRoyaltyDto> {

    override fun convert(source: ItemRoyalty): ItemRoyaltyDto {
        return ItemRoyaltyDto(
            contract = source.token,
            tokenId = source.tokenId.value,
            date = source.date,
            royalties = source.royalties.map { PartDtoConverter.convert(it) },
            owner = null,
            value = null
        )
    }
}