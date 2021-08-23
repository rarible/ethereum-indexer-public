package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.core.model.TokenStandard
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object CollectionTypeDtoConverter : Converter<TokenStandard, NftCollectionDto.Type> {
    override fun convert(source: TokenStandard): NftCollectionDto.Type {
        return when (source) {
            TokenStandard.ERC721, TokenStandard.DEPRECATED -> NftCollectionDto.Type.ERC721
            TokenStandard.ERC1155 -> NftCollectionDto.Type.ERC1155
            TokenStandard.CRYPTO_PUNKS -> NftCollectionDto.Type.CRYPTO_PUNKS
            TokenStandard.NONE -> throw IllegalArgumentException("Unexpected collection standard $source")
        }
    }
}
