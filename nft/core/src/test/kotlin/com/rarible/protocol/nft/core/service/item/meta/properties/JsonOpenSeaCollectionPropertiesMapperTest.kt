package com.rarible.protocol.nft.core.service.item.meta.properties

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.nft.core.model.TokenProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address

class JsonOpenSeaCollectionPropertiesMapperTest {

    private val collectionId = randomAddress()

    @Test
    fun `map correct json`() {
        val rawJson = """
            {
                "name": "NAME",
                "description": "DESCRIPTION",
                "external_link": "EXTERNAL_LINK",
                "image_url": "IMAGE_URL",
                "opensea_seller_fee_basis_points": 100,
                "payout_address": "0x40443ddfcaaadb9de1b8b96cc21304ad8c1c4b14"
            }
        """.trimIndent()

        val json = JsonPropertiesParser.parse("", rawJson)
        val properties = JsonOpenSeaCollectionPropertiesMapper.map(collectionId, json)

        assertThat(properties.name).isEqualTo("NAME")
        assertThat(properties.description).isEqualTo("DESCRIPTION")
        assertThat(properties.externalUri).isEqualTo("EXTERNAL_LINK")
        assertThat(properties.sellerFeeBasisPoints).isEqualTo(100)
        assertThat(properties.feeRecipient).isEqualTo(Address.apply("0x40443ddfcaaadb9de1b8b96cc21304ad8c1c4b14"))
        assertThat(properties.content.imageOriginal!!.url).isEqualTo("IMAGE_URL")
    }

    @Test
    fun `map empty json`() {
        val json = JsonPropertiesParser.parse("", "{}")
        val properties = JsonOpenSeaCollectionPropertiesMapper.map(collectionId, json)

        assertThat(properties).isEqualTo(TokenProperties.EMPTY)
    }

    @Test
    fun `map nested name - default name`() {
        val rawJson = """
            {"name": "Unidentified contract", "collection" : {"name": "abc"}}
        """.trimIndent()
        val json = JsonPropertiesParser.parse("", rawJson)
        val properties = JsonOpenSeaCollectionPropertiesMapper.map(collectionId, json)

        assertThat(properties.name).isEqualTo("abc")
    }

    @Test
    fun `map nested name - empty name`() {
        val rawJson = """
            {"name": "", "collection" : {"name": "abc"}}
        """.trimIndent()
        val json = JsonPropertiesParser.parse("", rawJson)
        val properties = JsonOpenSeaCollectionPropertiesMapper.map(collectionId, json)

        assertThat(properties.name).isEqualTo("abc")
    }

    @Test
    fun `map broken address`() {
        val json = JsonPropertiesParser.parse("", """{"payout_address": "ERROR"}""")
        val properties = JsonOpenSeaCollectionPropertiesMapper.map(collectionId, json)

        assertThat(properties.feeRecipient).isNull()
    }

    @Test
    fun `map empty address`() {
        val json = JsonPropertiesParser.parse("", """{"payout_address": " "}""")
        val properties = JsonOpenSeaCollectionPropertiesMapper.map(collectionId, json)

        assertThat(properties.feeRecipient).isNull()
    }

    @Test
    fun `map malformed address`() {
        // given
        val json = JsonPropertiesParser.parse("", """{"payout_address": "0x40"}""")

        // when
        val properties = JsonOpenSeaCollectionPropertiesMapper.map(collectionId, json)

        // then
        assertThat(properties.feeRecipient).isNull()
    }
}
