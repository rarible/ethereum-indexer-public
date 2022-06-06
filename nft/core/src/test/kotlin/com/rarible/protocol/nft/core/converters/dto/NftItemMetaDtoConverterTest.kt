package com.rarible.protocol.nft.core.converters.dto

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.ImageContentDto
import com.rarible.protocol.dto.MetaContentDto.Representation
import com.rarible.protocol.dto.NftItemAttributeDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.dto.VideoContentDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.model.ContentMeta
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemContentMeta
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemProperties
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

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
        val meta = ItemMeta(
            properties = ItemProperties(
                name = "name",
                description = "description",
                image = "http://test.com/abc_original",
                imagePreview = null,
                imageBig = "https://test.com//data:image/png;base64,aaa_base64",
                animationUrl = "http://test.com/abc_anim;data:svg/<svg test></svg>",
                attributes = listOf(
                    ItemAttribute(
                        key = randomString(),
                        value = randomString(),
                        type = randomString(),
                        format = randomString()
                    )
                ),
                rawJsonContent = randomString()
            ),
            itemContentMeta = ItemContentMeta(null, null)
        )

        val result = converter.convert(meta, itemId)

        val image = result.image!!
        val animation = result.animation!!

        assertThat(result.name).isEqualTo(meta.properties.name)
        assertThat(result.description).isEqualTo(meta.properties.description)

        assertThat(result.attributes!![0].key).isEqualTo(meta.properties.attributes[0].key)
        assertThat(result.attributes!![0].value).isEqualTo(meta.properties.attributes[0].value)
        assertThat(result.attributes!![0].format).isEqualTo(meta.properties.attributes[0].format)
        assertThat(result.attributes!![0].type).isEqualTo(meta.properties.attributes[0].type)

        assertThat(image.url["ORIGINAL"]).isEqualTo(meta.properties.image)
        assertThat(image.url["PREVIEW"]).isNull()
        assertThat(image.url["BIG"]).isEqualTo(
            "${basePublicApiUrl}items/$itemId/image?size=BIG&animation=false&hash=${meta.properties.imageBig!!.hashCode()}"
        )

        assertThat(animation.url["ORIGINAL"]).isEqualTo(
            "${basePublicApiUrl}items/$itemId/image?size=ORIGINAL&animation=true&hash=${meta.properties.animationUrl!!.hashCode()}"
        )
    }

    @Test
    fun `convert - large name and description`() {
        val itemId = createRandomItemId().decimalStringValue

        val meta = ItemMeta(
            properties = ItemProperties(
                name = "1234567890",
                description = "123456789_123456789",
                image = null,
                imagePreview = null,
                imageBig = null,
                animationUrl = null,
                attributes = listOf(),
                rawJsonContent = null,
            ),
            itemContentMeta = ItemContentMeta(null, null)
        )

        val result = converter.convert(meta, itemId)

        // 8 first chars in name plus '...'
        assertThat(result.name).isEqualTo("12345678...")
        // 16  first chars in description plus '...'
        assertThat(result.description).isEqualTo("123456789_123456...")
    }

    @Test
    fun `convert if all images exists`() {
        assertThat(
            converter.convert(innerMeta, itemId)
        ).isEqualTo(expectedMeta)
    }

    @Test
    fun `convert if previewImage is null`() {
        assertThat(
            converter.convert(
                innerMeta.copy(properties = innerMeta.properties.copy(imagePreview = null)),
                itemId
            )
        ).isEqualTo(
            expectedMeta.copy(
                image = NftMediaDto(
                    url = mapOf(
                        Pair("ORIGINAL", "http://test.com/abc_original"),
                        Pair("BIG", generatedImageUrl)
                    ),
                    meta = mapOf(
                        Pair("ORIGINAL", NftMediaMetaDto("jpeg", 100, 200))
                    )
                ),
                content = listOf(
                    ImageContentDto(
                        fileName = null,
                        url = "http://test.com/abc_original",
                        representation = Representation.ORIGINAL,
                        mimeType = "jpeg",
                        size = 300,
                        width = 100,
                        height = 200
                    ),
                    ImageContentDto(
                        fileName = null,
                        url = generatedImageUrl,
                        representation = Representation.BIG,
                        mimeType = null,
                        size = null,
                        width = null,
                        height = null
                    ),
                    VideoContentDto(
                        fileName = null,
                        url = generatedAnimationUrl,
                        representation = Representation.ORIGINAL,
                        mimeType = "mp4",
                        size = 300,
                        width = 200,
                        height = 400
                    )
                )
            )
        )
    }

    @Test
    fun `convert if original and preview Image is null`() {
        assertThat(
            converter.convert(
                innerMeta.copy(
                    properties = innerMeta.properties.copy(
                        imagePreview = null,
                        image = null
                    )
                ),
                itemId
            )
        ).isEqualTo(
            expectedMeta.copy(
                image = null,
                content = listOf(
                    VideoContentDto(
                        fileName = null,
                        url = generatedAnimationUrl,
                        representation = Representation.ORIGINAL,
                        mimeType = "mp4",
                        size = 300,
                        width = 200,
                        height = 400
                    )
                )
            )
        )
    }

    companion object {
        private const val basePublicApiUrl = "http://localhost/items/"
        private val itemMetaProperties = NftIndexerProperties.ItemMetaProperties(
            maxNameLength = 8,
            maxDescriptionLength = 16
        )

        private val createdAt = Instant.now()
        val itemId = createRandomItemId().decimalStringValue

        private const val embeddedImage = "https://test.com//data:image/png;base64,aaa_base64"
        val generatedImageUrl =
            "${basePublicApiUrl}items/$itemId/image?size=BIG&animation=false&hash=${embeddedImage.hashCode()}"
        private const val embeddedAnimation = "http://test.com/abc_anim;data:svg/<svg test></svg>"
        val generatedAnimationUrl =
            "${basePublicApiUrl}items/$itemId/image?size=ORIGINAL&animation=true&hash=${embeddedAnimation.hashCode()}"

        val innerMeta = ItemMeta(
            properties = ItemProperties(
                name = "name",
                description = "description",
                createdAt = createdAt,
                tags = listOf("tag1", "tag2"),
                genres = listOf("genre1", "genre2"),
                language = "lang",
                rights = "rights",
                rightsUri = "rightsUri",
                externalUri = "externalUri",
                image = "http://test.com/abc_original",
                imagePreview = "imagePreview",
                imageBig = embeddedImage,
                animationUrl = embeddedAnimation,
                attributes = listOf(
                    ItemAttribute(
                        key = "key1",
                        value = "value1",
                        type = "type1",
                        format = "format1"
                    ),
                    ItemAttribute(
                        key = "key2",
                        value = "value2",
                        type = "type2",
                        format = "format2"
                    )
                ),
                rawJsonContent = randomString(),
            ),
            itemContentMeta = ItemContentMeta(
                imageMeta = ContentMeta(
                    type = "jpeg",
                    width = 100,
                    height = 200,
                    size = 300
                ),
                animationMeta = ContentMeta(
                    type = "mp4",
                    width = 200,
                    height = 400,
                    size = 300
                )
            )
        )

        val expectedMeta = NftItemMetaDto(
            name = "name",
            description = "description",
            createdAt = createdAt,
            tags = listOf("tag1", "tag2"),
            genres = listOf("genre1", "genre2"),
            language = "lang",
            rights = "rights",
            rightsUri = "rightsUri",
            externalUri = "externalUri",
            attributes = listOf(
                NftItemAttributeDto(
                    key = "key1",
                    value = "value1",
                    type = "type1",
                    format = "format1"
                ),
                NftItemAttributeDto(
                    key = "key2",
                    value = "value2",
                    type = "type2",
                    format = "format2"
                )
            ),
            image = NftMediaDto(
                url = mapOf(
                    Pair("ORIGINAL", "http://test.com/abc_original"),
                    Pair("BIG", generatedImageUrl),
                    Pair("PREVIEW", "imagePreview")
                ),
                meta = mapOf(
                    Pair("PREVIEW", NftMediaMetaDto("jpeg", 100, 200))
                )
            ),
            animation = NftMediaDto(
                url = mapOf(
                    Pair("ORIGINAL", generatedAnimationUrl),
                ),
                meta = mapOf(
                    Pair("ORIGINAL", NftMediaMetaDto("mp4", 200, 400))
                )
            ),
            content = listOf(
                ImageContentDto(
                    fileName = null,
                    url = "http://test.com/abc_original",
                    representation = Representation.ORIGINAL,
                    mimeType = null,
                    size = null,
                    width = null,
                    height = null
                ),
                ImageContentDto(
                    fileName = null,
                    url = generatedImageUrl,
                    representation = Representation.BIG,
                    mimeType = null,
                    size = null,
                    width = null,
                    height = null
                ),
                ImageContentDto(
                    fileName = null,
                    url = "imagePreview",
                    representation = Representation.PREVIEW,
                    mimeType = "jpeg",
                    size = 300,
                    width = 100,
                    height = 200
                ),
                VideoContentDto(
                    fileName = null,
                    url = generatedAnimationUrl,
                    representation = Representation.ORIGINAL,
                    mimeType = "mp4",
                    size = 300,
                    width = 200,
                    height = 400
                )
            ),
        )
    }
}
