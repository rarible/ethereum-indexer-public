package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.AudioContentDto
import com.rarible.protocol.dto.HtmlContentDto
import com.rarible.protocol.dto.ImageContentDto
import com.rarible.protocol.dto.MetaContentDto
import com.rarible.protocol.dto.Model3dContentDto
import com.rarible.protocol.dto.UnknownContentDto
import com.rarible.protocol.dto.VideoContentDto
import com.rarible.protocol.nft.core.model.meta.EthAudioProperties
import com.rarible.protocol.nft.core.model.meta.EthHtmlProperties
import com.rarible.protocol.nft.core.model.meta.EthImageProperties
import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import com.rarible.protocol.nft.core.model.meta.EthModel3dProperties
import com.rarible.protocol.nft.core.model.meta.EthUnknownProperties
import com.rarible.protocol.nft.core.model.meta.EthVideoProperties

object EthMetaContentConverter {

    fun convert(contents: List<EthMetaContent>): List<MetaContentDto> {
        return contents.map { convert(it) }
    }

    fun convert(content: EthMetaContent): MetaContentDto {
        return when (val properties = content.properties) {
            is EthImageProperties -> ImageContentDto(
                url = content.url,
                representation = content.representation,
                fileName = content.fileName,
                mimeType = properties.mimeType,
                height = properties.height,
                size = properties.size,
                width = properties.width
            )
            is EthVideoProperties -> VideoContentDto(
                url = content.url,
                representation = content.representation,
                fileName = content.fileName,
                mimeType = properties.mimeType,
                height = properties.height,
                size = properties.size,
                width = properties.width
            )
            is EthAudioProperties -> AudioContentDto(
                url = content.url,
                representation = content.representation,
                fileName = content.fileName,
                mimeType = properties.mimeType,
                size = properties.size,
            )
            is EthModel3dProperties -> Model3dContentDto(
                url = content.url,
                representation = content.representation,
                fileName = content.fileName,
                mimeType = properties.mimeType,
                size = properties.size,
            )
            is EthHtmlProperties -> HtmlContentDto(
                url = content.url,
                representation = content.representation,
                fileName = content.fileName,
                mimeType = properties.mimeType,
                size = properties.size,
            )
            is EthUnknownProperties -> UnknownContentDto(
                url = content.url,
                representation = content.representation,
                fileName = null,
                mimeType = null,
                size = null
            )
            else -> UnknownContentDto(
                url = content.url,
                representation = MetaContentDto.Representation.ORIGINAL,
                fileName = null,
                mimeType = null,
                size = null
            )
        }
    }
}
