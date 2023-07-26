package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.ItemRoyaltyDto
import com.rarible.protocol.nft.core.model.ItemRoyalty

object ItemRoyaltyDtoConverter {

    fun convert(source: ItemRoyalty): ItemRoyaltyDto {
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
