package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.core.model.TokenStandard

object CollectionTypeDtoConverter {

    fun convert(source: TokenStandard): NftCollectionDto.Type {
        return when (source) {
            TokenStandard.ERC721, TokenStandard.DEPRECATED -> NftCollectionDto.Type.ERC721
            TokenStandard.ERC1155 -> NftCollectionDto.Type.ERC1155
            TokenStandard.CRYPTO_PUNKS -> NftCollectionDto.Type.CRYPTO_PUNKS
            TokenStandard.NONE -> throw IllegalArgumentException("Unexpected collection standard $source")
        }
    }
}
