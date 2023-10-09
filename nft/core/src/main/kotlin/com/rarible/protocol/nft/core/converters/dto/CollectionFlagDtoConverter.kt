package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.EthCollectionFlagsDto
import com.rarible.protocol.nft.core.model.TokenFlags

object CollectionFlagDtoConverter {
    fun convert(source: TokenFlags): EthCollectionFlagsDto =
        EthCollectionFlagsDto(paused = source.paused)
}
