package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftDeletedOwnershipDto
import com.rarible.protocol.nft.core.model.OwnershipId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object DeletedOwnershipDtoConverter : Converter<OwnershipId, NftDeletedOwnershipDto> {
    override fun convert(source: OwnershipId): NftDeletedOwnershipDto {
        return NftDeletedOwnershipDto(
            id = source.stringValue,
            token = source.token,
            tokenId = source.tokenId.value,
            owner = source.owner
        )
    }
}

