package com.rarible.protocol.nft.core.converters.dto

import com.rarible.core.test.data.randomString
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemContentMeta
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemProperties
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NftItemMetaDtoConverterTest {

    private val basePublicApiUrl = "http://localhost/items/"
    private val properties: NftIndexerProperties = mockk()
    private lateinit var converter: NftItemMetaDtoConverter

    private val itemMetaProperties = NftIndexerProperties.ItemMetaProperties(
        maxNameLength = 8,
        maxDescriptionLength = 16
    )

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
                animationUrl = "http://test.com/abc_anim",
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
        assertThat(image.url["BIG"]).isEqualTo("${basePublicApiUrl}items/$itemId/image?size=BIG&hash=${meta.properties.imageBig!!.hashCode()}")

        assertThat(animation.url["ORIGINAL"]).isEqualTo(meta.properties.animationUrl)
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
}