package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftDeletedItemDto
import com.rarible.protocol.nft.core.model.ItemId

object DeletedItemDtoConverter {

    fun convert(source: ItemId): NftDeletedItemDto {
        return NftDeletedItemDto(
            id = source.decimalStringValue,
            token = source.token,
            tokenId = source.tokenId.value
        )
    }
}
