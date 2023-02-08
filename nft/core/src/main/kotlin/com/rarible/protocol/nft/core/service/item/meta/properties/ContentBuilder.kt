package com.rarible.protocol.nft.core.service.item.meta.properties

import com.rarible.core.common.ifNotBlank
import com.rarible.protocol.dto.MetaContentDto.Representation
import com.rarible.protocol.nft.core.model.ContentMeta
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
        imagePortrait: String? = null,
        videoOriginal: String? = null
    ): ItemMetaContent {
        return ItemMetaContent(
            imageOriginal = imageOriginal?.cleanUrl()?.let { toImage(it, Representation.ORIGINAL) },
            imageBig = imageBig?.cleanUrl()?.let { toImage(it, Representation.BIG) },
            imagePreview = imagePreview?.cleanUrl()?.let { toImage(it, Representation.PREVIEW) },
            imagePortrait = imagePortrait?.cleanUrl()?.let { toImage(it, Representation.PORTRAIT) },
            videoOriginal = videoOriginal?.cleanUrl()?.let { toVideo(it, Representation.ORIGINAL) }
        )
    }

    fun getTokenMetaContent(
        imageOriginal: String? = null
    ): TokenMetaContent {
        return TokenMetaContent(
            imageOriginal = imageOriginal?.cleanUrl()?.let { toImage(it, Representation.ORIGINAL) },
        )
    }

    fun String?.cleanUrl() = this?.trim()?.ifNotBlank()

    // TODO Remove after moving calculation Token content meta to Union
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

    private fun toVideo(url: String, representation: Representation): EthMetaContent {
        return EthMetaContent(
            url = url,
            representation = representation,
            properties = EthVideoProperties()
        )
    }

    private fun toImage(url: String, representation: Representation): EthMetaContent {
        return EthMetaContent(
            url = url,
            representation = representation,
            properties = EthImageProperties()
        )
    }
}
