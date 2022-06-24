package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftCollectionMetaDto
import com.rarible.protocol.nft.core.model.TokenMeta
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object NftCollectionMetaDtoConverter : Converter<TokenMeta, NftCollectionMetaDto> {
    override fun convert(source: TokenMeta): NftCollectionMetaDto {
        return NftCollectionMetaDto(
            name = source.properties.name,
            description = source.properties.description,
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

            content = source.properties.content.asList().map { EthMetaContentConverter.convert(it) },

            // TODO legacy fields, remove
            external_link = source.properties.externalUri,
            seller_fee_basis_points = source.properties.sellerFeeBasisPoints,
            fee_recipient = source.properties.feeRecipient
        )
    }

}
