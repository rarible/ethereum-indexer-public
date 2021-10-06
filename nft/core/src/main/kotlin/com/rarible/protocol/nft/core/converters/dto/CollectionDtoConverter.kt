package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object CollectionDtoConverter : Converter<Token, NftCollectionDto> {
    override fun convert(source: Token): NftCollectionDto {
        return NftCollectionDto(
            id = source.id,
            type = CollectionTypeDtoConverter.convert(source.standard),
            owner = source.owner,
            name = source.name,
            symbol = source.symbol,
            features = source.features.map { CollectionFeatureDtoConverter.convert(it) },
            supportsLazyMint = source.features.contains(TokenFeature.MINT_AND_TRANSFER),
            minters = source.minters.ifEmpty { listOfNotNull(source.owner) }
        )
    }
}
