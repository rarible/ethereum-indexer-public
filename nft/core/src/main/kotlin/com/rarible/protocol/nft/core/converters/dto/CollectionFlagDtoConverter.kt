package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftCollectionFlagDto
import com.rarible.protocol.nft.core.model.TokenFlag

object CollectionFlagDtoConverter {
    fun convert(source: Map<TokenFlag, String>): List<NftCollectionFlagDto> =
        source.entries.map { NftCollectionFlagDto(convert(it.key), it.value) }

    private fun convert(tokenFlag: TokenFlag): NftCollectionFlagDto.Flag =
        when (tokenFlag) {
            TokenFlag.PAUSED -> NftCollectionFlagDto.Flag.PAUSED
        }
}
