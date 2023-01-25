package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftCollectionMetaDto
import com.rarible.protocol.nft.core.model.TokenMeta

object NftCollectionMetaDtoConverter {

    fun convert(source: TokenMeta): NftCollectionMetaDto {
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
        )
    }

}
