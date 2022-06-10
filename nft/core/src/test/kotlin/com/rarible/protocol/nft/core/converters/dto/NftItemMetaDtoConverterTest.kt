package com.rarible.protocol.nft.core.converters.dto

import com.rarible.core.meta.resource.detector.ContentDetector
import com.rarible.core.meta.resource.detector.embedded.EmbeddedContentDetector
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.MetaContentDto.Representation
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.data.randomItemProperties
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemContentMeta
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.meta.EthImageProperties
import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import com.rarible.protocol.nft.core.model.meta.EthVideoProperties
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
        converter = NftItemMetaDtoConverter(
            nftIndexerProperties = properties,
            embeddedContentDetector = EmbeddedContentDetector(ContentDetector())
        )
    }

    @Test
    fun convert() {
        val itemId = createRandomItemId().decimalStringValue

        val animationUrl = "http://test.com/abc_anim;data:svg/<svg test></svg>"
        val imageUrl = "http://test.com/abc_original"
        val imageBigUrl = "https://test.com//data:image/png;base64,aaa_base64"

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
                image = imageUrl,
                imagePreview = null,
                imageBig = imageBigUrl,
                animationUrl = animationUrl,
                tokenUri = "tokenUri",
                attributes = listOf(attribute),
                rawJsonContent = randomString(),
                content = listOf()
            ),
            itemContentMeta = ItemContentMeta(null, null)
        )

        val result = converter.convert(meta, itemId)

        val convertedImage = result.image!!
        val convertedAnimation = result.animation!!

        assertThat(result.name).isEqualTo(meta.properties.name)
        assertThat(result.description).isEqualTo(meta.properties.description)
        assertThat(result.originalMetaUri).isEqualTo(meta.properties.tokenUri)

        val convertedAttribute = result.attributes!![0]
        assertThat(convertedAttribute.key).isEqualTo(attribute.key)
        assertThat(convertedAttribute.value).isEqualTo(attribute.value)
        assertThat(convertedAttribute.format).isEqualTo(attribute.format)
        assertThat(convertedAttribute.type).isEqualTo(attribute.type)

        assertThat(convertedImage.url["ORIGINAL"]).isEqualTo(meta.properties.image)
        assertThat(convertedImage.url["PREVIEW"]).isNull()
        assertThat(convertedImage.url["BIG"]).isEqualTo(
            "${basePublicApiUrl}items/$itemId/image?size=BIG&animation=false&hash=${meta.properties.imageBig!!.hashCode()}"
        )

        assertThat(convertedAnimation.url["ORIGINAL"]).isEqualTo(
            "${basePublicApiUrl}items/$itemId/image?size=ORIGINAL&animation=true&hash=${meta.properties.animationUrl!!.hashCode()}"
        )
    }

    @Test
    fun `convert - with new content`() {
        val animationUrl = "http://test.com/abc_anim;data:svg/<svg test></svg>"
        val imageUrl = "http://test.com/abc_original"
        val imageBigUrl = "https://test.com//data:image/png;base64,aaa_base64"

        val image = EthMetaContent(
            url = imageUrl,
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
            url = animationUrl,
            representation = Representation.ORIGINAL,
            fileName = randomString(),
            properties = EthVideoProperties()
        )

        val meta = ItemMeta(
            properties = randomItemProperties().copy(content = listOf(image, imageBig, video)),
            itemContentMeta = ItemContentMeta(null, null)
        )

        val result = converter.convert(meta, itemId)

        val convertedImage = result.content[0]
        val convertedImageBig = result.content[1]
        val convertedVideo = result.content[2]

        // In modern content URLs should stay the same, without embedding
        assertThat(convertedImage.url).isEqualTo(imageUrl)
        assertThat(convertedImageBig.url).isEqualTo(imageBigUrl)
        assertThat(convertedVideo.url).isEqualTo(animationUrl)
    }

    @Test
    fun `convert - large name and description`() {
        val itemId = createRandomItemId().decimalStringValue

        val meta = ItemMeta(
            properties = randomItemProperties().copy(
                name = "1234567890",
                description = "123456789_123456789"
            ),
            itemContentMeta = ItemContentMeta(null, null)
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
    }
}
