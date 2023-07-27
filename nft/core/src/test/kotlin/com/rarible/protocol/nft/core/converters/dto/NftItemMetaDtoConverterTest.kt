package com.rarible.protocol.nft.core.converters.dto

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.ImageContentDto
import com.rarible.protocol.dto.MetaContentDto.Representation
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.VideoContentDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.data.randomItemProperties
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.ItemMetaContent
import com.rarible.protocol.nft.core.model.meta.EthImageProperties
import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import com.rarible.protocol.nft.core.model.meta.EthVideoProperties
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NftItemMetaDtoConverterTest {

    private lateinit var converter: NftItemMetaDtoConverter
    private val properties: NftIndexerProperties = mockk()

    @BeforeEach
    fun beforeEach() {
        every { properties.basePublicApiUrl } returns basePublicApiUrl
        every { properties.itemMeta } returns itemMetaProperties
        converter = NftItemMetaDtoConverter(properties)
    }

    @Test
    fun convert() {
        val itemId = createRandomItemId().decimalStringValue

        val imageOriginalUrl = "http://test.com/abc_original"
        val imageBigUrl = "https://test.com//data:image/png;base64,aaa_base64"
        val videoOriginalUrl = "http://test.com/abc_anim;data:svg/<svg test></svg>"

        val attribute = ItemAttribute(
            key = randomString(),
            value = randomString(),
            type = randomString(),
            format = randomString()
        )

        val meta = ItemMeta(
            properties = ItemProperties(
                name = "name",
                description = "description",
                tokenUri = "tokenUri",
                attributes = listOf(attribute),
                rawJsonContent = randomString(),
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = imageOriginalUrl,
                    imageBig = imageBigUrl,
                    imagePreview = null,
                    videoOriginal = videoOriginalUrl
                )
            )
        )

        val nftItemMetaDto = converter.convert(meta, itemId)
        val parsedMetaDto = nftItemMetaDto.itemProperties()

        val convertedImageOriginal = parsedMetaDto.content.imageOriginal
        val convertedImageBig = parsedMetaDto.content.imageBig
        val convertedImagePreview = parsedMetaDto.content.imagePreview
        val convertedVideoOriginal = parsedMetaDto.content.videoOriginal

        assertThat(nftItemMetaDto.name).isEqualTo(meta.properties.name)
        assertThat(nftItemMetaDto.description).isEqualTo(meta.properties.description)
        assertThat(nftItemMetaDto.originalMetaUri).isEqualTo(meta.properties.tokenUri)

        val convertedAttribute = nftItemMetaDto.attributes!![0]
        assertThat(convertedAttribute.key).isEqualTo(attribute.key)
        assertThat(convertedAttribute.value).isEqualTo(attribute.value)
        assertThat(convertedAttribute.format).isEqualTo(attribute.format)
        assertThat(convertedAttribute.type).isEqualTo(attribute.type)

        assertThat(convertedImageOriginal).isEqualTo(meta.properties.content.imageOriginal)
        assertThat(convertedImageBig).isEqualTo(meta.properties.content.imageBig)
        assertThat(convertedImagePreview).isEqualTo(meta.properties.content.imagePreview)
        assertThat(convertedVideoOriginal).isEqualTo(meta.properties.content.videoOriginal)
    }

    @Test
    fun `convert - with new content`() {
        val imageOriginalUrl = "http://test.com/abc_original"
        val imageBigUrl = "https://test.com//data:image/png;base64,aaa_base64"
        val videoOriginalUrl = "http://test.com/abc_anim;data:svg/<svg test></svg>"

        val image = EthMetaContent(
            url = imageOriginalUrl,
            representation = Representation.ORIGINAL,
            fileName = randomString(),
            properties = EthImageProperties()
        )

        val imageBig = EthMetaContent(
            url = imageBigUrl,
            representation = Representation.BIG,
            fileName = randomString(),
            properties = EthImageProperties()
        )

        val video = EthMetaContent(
            url = videoOriginalUrl,
            representation = Representation.ORIGINAL,
            fileName = randomString(),
            properties = EthVideoProperties()
        )

        val meta = ItemMeta(
            properties = randomItemProperties().copy(
                content = ItemMetaContent(
                    imageOriginal = image,
                    imageBig = imageBig,
                    imagePreview = null,
                    videoOriginal = video
                )
            )
        )

        val result = converter.convert(meta, itemId)

        val convertedImage = result.content[0]
        val convertedImageBig = result.content[1]
        val convertedVideo = result.content[2]

        // In modern content URLs should stay the same, without embedding
        assertThat(convertedImage.url).isEqualTo(imageOriginalUrl)
        assertThat(convertedImageBig.url).isEqualTo(imageBigUrl)
        assertThat(convertedVideo.url).isEqualTo(videoOriginalUrl)
    }

    @Test
    fun `convert - large name and description`() {
        val itemId = createRandomItemId().decimalStringValue

        val meta = ItemMeta(
            properties = randomItemProperties().copy(
                name = "1234567890",
                description = "123456789_123456789"
            )
        )

        val result = converter.convert(meta, itemId)

        // 8 first chars in name plus '...'
        assertThat(result.name).isEqualTo("12345678...")
        // 16  first chars in description plus '...'
        assertThat(result.description).isEqualTo("123456789_123456...")
    }

    companion object {

        private const val basePublicApiUrl = "http://localhost/items/"
        private val itemMetaProperties = NftIndexerProperties.ItemMetaProperties(
            maxNameLength = 8,
            maxDescriptionLength = 16
        )
        val itemId = createRandomItemId().decimalStringValue

        fun NftItemMetaDto.itemProperties(): ItemProperties {

            var imageOriginal: EthMetaContent? = null
            var imageBig: EthMetaContent? = null
            var imagePreview: EthMetaContent? = null
            var videoOriginal: EthMetaContent? = null

            this.content.forEach {

                val metaContent = EthMetaContent(
                    url = it.url,
                    representation = it.representation,
                    fileName = it.fileName,
                    properties = if (it is ImageContentDto) {
                        EthImageProperties(
                            mimeType = it.mimeType,
                            size = it.size,
                            width = it.width,
                            height = it.height
                        )
                    } else if (it is VideoContentDto) {
                        EthVideoProperties(
                            mimeType = it.mimeType,
                            size = it.size,
                            width = it.width,
                            height = it.height
                        )
                    } else {
                        null
                    }
                )

                when {
                    it is VideoContentDto && it.representation == Representation.ORIGINAL -> videoOriginal = metaContent
                    it is ImageContentDto && it.representation == Representation.ORIGINAL -> imageOriginal = metaContent
                    it is ImageContentDto && it.representation == Representation.PREVIEW -> imagePreview = metaContent
                    it is ImageContentDto && it.representation == Representation.BIG -> imageBig = metaContent
                }
            }

            return ItemProperties(
                name = name,
                description = description,
                attributes = attributes.orEmpty().map {
                    ItemAttribute(
                        key = it.key,
                        value = it.value,
                        type = it.type,
                        format = it.format
                    )
                },
                rawJsonContent = null,
                content = ItemMetaContent(
                    imageOriginal = imageOriginal,
                    imageBig = imageBig,
                    imagePreview = imagePreview,
                    videoOriginal = videoOriginal
                )
            )
        }
    }
}
