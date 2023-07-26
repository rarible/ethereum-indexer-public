package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.nft.core.model.Item

object ItemDtoConverter {

    fun convert(item: Item): NftItemDto =
        NftItemDto(
            id = item.id.decimalStringValue,
            contract = item.token,
            tokenId = item.tokenId.value,
            creators = item.creators.map { PartDtoConverter.convert(it) },
            supply = item.supply.value,
            lazySupply = item.lazySupply.value,
            lastUpdatedAt = item.date,
            mintedAt = item.mintedAt,
            deleted = item.deleted,
            isRaribleContract = item.isRaribleContract,
            isSuspiciousOnOS = item.isSuspiciousOnOS,
        )
}
