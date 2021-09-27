package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.*
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.MediaMeta
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object NftItemMetaDtoConverter : Converter<ItemMeta, NftItemMetaDto> {
    override fun convert(source: ItemMeta): NftItemMetaDto {
        return NftItemMetaDto(
            name = source.properties.name,
            description = source.properties.description,
            attributes = source.properties.attributes.map { convert(it) },
            image = createImageMedia(source),
            animation = createAnimationMedia(source)
        )
    }

    private fun createImageMedia(source: ItemMeta): NftMediaDto? {
        return if (source.properties.imagePreview != null || source.properties.image != null) {
            val url = source.properties.toUrlMap(NftMediaSizeDto.ORIGINAL) { it.image } +
                    source.properties.toUrlMap(NftMediaSizeDto.BIG) { it.imageBig } +
                    source.properties.toUrlMap(NftMediaSizeDto.PREVIEW) { it.imagePreview }

            val meta = source.meta.imageMeta
                ?.let { convert(it) }
                ?.let { mapOf((if (source.properties.imagePreview != null) NftMediaSizeDto.PREVIEW.name else NftMediaSizeDto.ORIGINAL.name) to it) }
                ?: emptyMap()
            NftMediaDto(url, meta)
        } else {
            null
        }
    }

    private fun createAnimationMedia(source: ItemMeta): NftMediaDto? {
        return if (source.properties.animationUrl != null) {
            val url = source.properties.toUrlMap(NftMediaSizeDto.ORIGINAL) { it.animationUrl }

            val meta = source.meta.animationMeta
                ?.let { convert(it) }
                ?.let { mapOf(NftMediaSizeDto.ORIGINAL.name to it) }
                ?: emptyMap()
            NftMediaDto(url, meta)
        } else {
            null
        }
    }

    private fun ItemProperties.toUrlMap(size: NftMediaSizeDto, extractor: (ItemProperties) -> String?) =
        extractor(this)
            ?.let { mapOf(size.name to it) }
            ?: emptyMap()

    private fun convert(source: MediaMeta): NftMediaMetaDto {
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
}
