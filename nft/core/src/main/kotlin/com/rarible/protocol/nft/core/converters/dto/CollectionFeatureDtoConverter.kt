package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.core.model.TokenFeature
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object CollectionFeatureDtoConverter : Converter<TokenFeature, NftCollectionDto.Features> {
    override fun convert(source: TokenFeature): NftCollectionDto.Features {
        return when (source) {
            TokenFeature.APPROVE_FOR_ALL -> NftCollectionDto.Features.APPROVE_FOR_ALL
            TokenFeature.SET_URI_PREFIX -> NftCollectionDto.Features.SET_URI_PREFIX
            TokenFeature.BURN -> NftCollectionDto.Features.BURN
            TokenFeature.MINT_WITH_ADDRESS -> NftCollectionDto.Features.MINT_WITH_ADDRESS
            TokenFeature.SECONDARY_SALE_FEES -> NftCollectionDto.Features.SECONDARY_SALE_FEES
            TokenFeature.MINT_AND_TRANSFER -> NftCollectionDto.Features.MINT_AND_TRANSFER
        }
    }
}
