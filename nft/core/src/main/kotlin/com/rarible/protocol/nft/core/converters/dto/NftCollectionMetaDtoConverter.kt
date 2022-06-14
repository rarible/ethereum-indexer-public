package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftCollectionMetaDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.dto.NftMediaSizeDto
import com.rarible.protocol.nft.core.model.ContentMeta
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
            createdAt = source.properties.createdAt,
            tags = source.properties.tags,
            genres = source.properties.genres,
            language = source.properties.language,
            rights = source.properties.rights,
            rightsUri = source.properties.rightsUri,
            externalUri = source.properties.externalUri,
            originalMetaUri = source.properties.tokenUri,
            sellerFeeBasisPoints = source.properties.sellerFeeBasisPoints,
            feeRecipient = source.properties.feeRecipient,

            content = source.properties.content.map { EthMetaContentConverter.convert(it) },

            // TODO legacy fields, remove
            external_link = source.properties.externalUri,
            seller_fee_basis_points = source.properties.sellerFeeBasisPoints,
            fee_recipient = source.properties.feeRecipient
        )
    }

    private fun createImage(source: TokenMeta): NftMediaDto? {
        return if (source.properties.image != null) {
            NftMediaDto(
                url = mapOf(NftMediaSizeDto.ORIGINAL.toString() to source.properties.image),
                meta = if (source.contentMeta != null) {
                    mapOf(NftMediaSizeDto.ORIGINAL.toString() to convert(source.contentMeta))
                } else {
                    emptyMap()
                }
            )
        } else {
            null
        }
    }

    private fun convert(source: ContentMeta): NftMediaMetaDto {
        return NftMediaMetaDto(
            type = source.type,
            width = source.width,
            height = source.height
        )
    }
}
