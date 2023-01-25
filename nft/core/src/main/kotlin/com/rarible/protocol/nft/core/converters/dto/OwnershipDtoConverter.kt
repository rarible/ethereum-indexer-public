package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.nft.core.model.Ownership

object OwnershipDtoConverter {

    fun convert(source: Ownership): NftOwnershipDto {
        return NftOwnershipDto(
            id = source.id.decimalStringValue,
            contract = source.token,
            tokenId = source.tokenId.value,
            owner = source.owner,
            creators = source.creators.map { PartDtoConverter.convert(it) },
            value = source.value.value,
            lazyValue = source.lazyValue.value,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            pending = emptyList()
        )
    }
}
