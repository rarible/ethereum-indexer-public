package com.rarible.protocol.nftorder.core.converter

import com.rarible.protocol.dto.NftOrderOwnershipDto
import com.rarible.protocol.dto.NftOwnershipDto

object NftOwnershipDtoToNftOrderOwnershipDtoConverter {

    fun convert(dto: NftOwnershipDto): NftOrderOwnershipDto {
        return NftOrderOwnershipDto(
            id = dto.id,
            contract = dto.contract,
            tokenId = dto.tokenId,
            creators = dto.creators,
            owner = dto.owner,
            value = dto.value,
            lazyValue = dto.lazyValue,
            date = dto.date,
            pending = dto.pending,
            bestSellOrder = null
        )
    }
}