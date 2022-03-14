package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftItemAttributeDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.dto.NftMediaSizeDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.misc.detector.EmbeddedImageDetector
import com.rarible.protocol.nft.core.misc.trimToLength
import com.rarible.protocol.nft.core.model.ContentMeta
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemProperties
import org.springframework.stereotype.Component

@Component
class NftItemMetaDtoConverter(
    nftIndexerProperties: NftIndexerProperties
) {

    val baseImageUrl = getBaseImageUrl(nftIndexerProperties.basePublicApiUrl)
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
            image = createImageMedia(source, itemIdDecimalValue),
            animation = createAnimationMedia(source, itemIdDecimalValue)
        )
    }

    private fun createImageMedia(source: ItemMeta, itemIdDecimalValue: String): NftMediaDto? {
        return if (source.properties.imagePreview != null || source.properties.image != null) {
            val url = source.properties.toUrlMap(NftMediaSizeDto.ORIGINAL, itemIdDecimalValue, false) { it.image } +
                source.properties.toUrlMap(NftMediaSizeDto.BIG, itemIdDecimalValue, false) { it.imageBig } +
                source.properties.toUrlMap(NftMediaSizeDto.PREVIEW, itemIdDecimalValue, false) { it.imagePreview }

            val meta = source.itemContentMeta.imageMeta
                ?.let { convert(it) }
                ?.let { mapOf((if (source.properties.imagePreview != null) NftMediaSizeDto.PREVIEW.name else NftMediaSizeDto.ORIGINAL.name) to it) }
                ?: emptyMap()
            NftMediaDto(url, meta)
        } else {
            null
        }
    }

    private fun createAnimationMedia(source: ItemMeta, itemIdDecimalValue: String): NftMediaDto? {
        return if (source.properties.animationUrl != null) {
            val url = source.properties.toUrlMap(NftMediaSizeDto.ORIGINAL, itemIdDecimalValue, true) { it.animationUrl }

            val meta = source.itemContentMeta.animationMeta
                ?.let { convert(it) }
                ?.let { mapOf(NftMediaSizeDto.ORIGINAL.name to it) }
                ?: emptyMap()
            NftMediaDto(url, meta)
        } else {
            null
        }
    }

    private fun ItemProperties.toUrlMap(
        size: NftMediaSizeDto,
        itemIdDecimalValue: String,
        isAnimation: Boolean,
        extractor: (ItemProperties) -> String?
    ): Map<String, String> {
        val url = extractor(this)
        return url?.let {
            mapOf(size.name to sanitizeNestedImage(it, size.name, itemIdDecimalValue, isAnimation))
        } ?: emptyMap()
    }

    private fun sanitizeNestedImage(url: String, size: String, itemIdDecimalValue: String, isAnimation: Boolean): String {
        EmbeddedImageDetector.getDetector(url) ?: return url
        return "$baseImageUrl/$itemIdDecimalValue/image?size=$size&animation=${isAnimation}&hash=${url.hashCode()}"
    }

    private fun convert(source: ContentMeta): NftMediaMetaDto {
        return NftMediaMetaDto(
            type = source.type,
            width = source.width,
            height = source.height
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

    private fun getBaseImageUrl(basePublicApiUrl: String): String {
        val withoutSlash = basePublicApiUrl.trimEnd('/')
        return "$withoutSlash/items"
    }
}
