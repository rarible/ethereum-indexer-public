package com.rarible.protocol.nft.core.service.item.meta.properties

import com.rarible.protocol.dto.MetaContentDto
import com.rarible.protocol.nft.core.model.ContentMeta
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMetaContent
import com.rarible.protocol.nft.core.model.TokenMetaContent
import com.rarible.protocol.nft.core.model.meta.EthImageProperties
import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import com.rarible.protocol.nft.core.model.meta.EthVideoProperties

object ContentBuilder {

    fun getItemMetaContent(
        imageOriginal: String? = null,
        imageBig: String? = null,
        imagePreview: String? = null,
        videoOriginal: String? = null
    ): ItemMetaContent {
        return ItemMetaContent(
            imageOriginal = imageOriginal?.let { toImage(it, MetaContentDto.Representation.ORIGINAL) },
            imageBig = imageBig?.let { toImage(it, MetaContentDto.Representation.BIG) },
            imagePreview = imagePreview?.let { toImage(it, MetaContentDto.Representation.PREVIEW) },
            videoOriginal = videoOriginal?.let { toVideo(it, MetaContentDto.Representation.ORIGINAL) }
        )
    }

    fun getTokenMetaContent(
        imageOriginal: String? = null
    ): TokenMetaContent {
        return TokenMetaContent(
            imageOriginal = imageOriginal?.let { toImage(it, MetaContentDto.Representation.ORIGINAL) },
        )
    }

    fun populateContent(ethMetaContent: EthMetaContent?, data: ContentMeta?): EthMetaContent? {
        return ethMetaContent?.copy(
            properties = EthImageProperties(
                mimeType = data?.type,
                size = data?.size,
                width = data?.width,
                height = data?.height
            )
        )
    }

    private fun sanitize(itemId: ItemId, url: String?): String? { // TODO Suppose not need
        if (url == null) {
            return null
        }

        val svg = SvgSanitizer.sanitize(itemId, url)

        return svg ?: url
    }

    private fun toVideo(url: String, representation: MetaContentDto.Representation): EthMetaContent {
        return EthMetaContent(
            url = url,
            representation = representation,
            properties = EthVideoProperties()
        )
    }

    private fun toImage(url: String, representation: MetaContentDto.Representation): EthMetaContent {
        return EthMetaContent(
            url = url,
            representation = representation,
            properties = EthImageProperties()
        )
    }
}
