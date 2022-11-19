package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftItemAttributeDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.misc.trimToLength
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemMeta
import org.springframework.stereotype.Component

@Component
class NftItemMetaDtoConverter(
    nftIndexerProperties: NftIndexerProperties
) {

    val maxNameLength = nftIndexerProperties.itemMeta.maxNameLength
    val maxDescriptionLength = nftIndexerProperties.itemMeta.maxDescriptionLength

    fun convert(source: ItemMeta, itemIdDecimalValue: String): NftItemMetaDto {
        val trimmedName = trimToLength(source.properties.name, maxNameLength, "...")
        val trimmedDescription = source.properties.description?.let {
            trimToLength(it, maxDescriptionLength, "...")
        }
        return NftItemMetaDto(
            name = trimmedName,
            description = trimmedDescription,
            attributes = source.properties.attributes.map { convert(it) },
            createdAt = source.properties.createdAt,
            tags = source.properties.tags,
            genres = source.properties.genres,
            language = source.properties.language,
            rights = source.properties.rights,
            rightsUri = source.properties.rightsUri,
            externalUri = source.properties.externalUri,
            content = source.properties.content.asList().map { EthMetaContentConverter.convert(it) },
            originalMetaUri = source.properties.tokenUri,
            status = NftItemMetaDto.Status.OK
        )
    }

    private fun convert(source: ItemAttribute): NftItemAttributeDto {
        return NftItemAttributeDto(
            key = source.key,
            value = source.value,
            type = source.type,
            format = source.format
        )
    }
}
