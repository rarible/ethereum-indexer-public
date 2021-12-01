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
            name = source.name,
            description = source.description,
            image = createImage(source),
            external_link = source.external_link,
            seller_fee_basis_points = source.seller_fee_basis_points,
            fee_recipient = source.fee_recipient
        )
    }

    private fun createImage(source: TokenMeta): NftMediaDto? {
        if (source.image != null && source.imageMeta != null) {
            return NftMediaDto(
                url = mapOf(NftMediaSizeDto.ORIGINAL.toString() to source.image!!),
                meta = mapOf(NftMediaSizeDto.ORIGINAL.toString() to convert(source.imageMeta!!))
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
