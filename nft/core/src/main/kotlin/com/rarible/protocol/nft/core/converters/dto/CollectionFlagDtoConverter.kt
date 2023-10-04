package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.EthCollectionFlagDto
import com.rarible.protocol.nft.core.model.TokenFlag

object CollectionFlagDtoConverter {
    fun convert(source: Map<TokenFlag, String>): List<EthCollectionFlagDto> =
        source.entries.map { EthCollectionFlagDto(convert(it.key), it.value) }

    private fun convert(tokenFlag: TokenFlag): EthCollectionFlagDto.Flag =
        when (tokenFlag) {
            TokenFlag.PAUSED -> EthCollectionFlagDto.Flag.PAUSED
        }
}
