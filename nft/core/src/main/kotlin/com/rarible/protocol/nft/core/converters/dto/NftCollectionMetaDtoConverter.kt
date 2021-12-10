package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftCollectionMetaDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.dto.NftMediaSizeDto
import com.rarible.protocol.nft.core.model.MediaMeta
import com.rarible.protocol.nft.core.model.TokenMeta
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object NftCollectionMetaDtoConverter : Converter<TokenMeta, NftCollectionMetaDto> {
    override fun convert(source: TokenMeta): NftCollectionMetaDto {
        return NftCollectionMetaDto(
            name = source.properties.name,
            description = source.properties.description,
            image = createImage(source),
            external_link = source.properties.externalLink,
            seller_fee_basis_points = source.properties.sellerFeeBasisPoints,
            fee_recipient = source.properties.feeRecipient
        )
    }

    private fun createImage(source: TokenMeta): NftMediaDto? {
        if (source.properties.image != null && source.contentMeta != null) {
            return NftMediaDto(
                url = mapOf(NftMediaSizeDto.ORIGINAL.toString() to source.properties.image),
                meta = mapOf(NftMediaSizeDto.ORIGINAL.toString() to convert(source.contentMeta))
            )
        } else {
            return null
        }
    }

    private fun convert(source: MediaMeta): NftMediaMetaDto {
        return NftMediaMetaDto(
            type = source.type,
            width = source.width,
            height = source.height
        )
    }
}
