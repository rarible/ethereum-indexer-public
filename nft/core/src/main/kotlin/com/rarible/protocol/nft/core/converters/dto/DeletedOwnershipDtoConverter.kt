package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftDeletedOwnershipDto
import com.rarible.protocol.nft.core.model.OwnershipId

@Deprecated("NftDeletedOwnershipDto should be removed")
object DeletedOwnershipDtoConverter {

    fun convert(source: OwnershipId): NftDeletedOwnershipDto {
        return NftDeletedOwnershipDto(
            id = source.stringValue,
            token = source.token,
            tokenId = source.tokenId.value,
            owner = source.owner
        )
    }
}

