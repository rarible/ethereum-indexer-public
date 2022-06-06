package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.ImageContentDto
import com.rarible.protocol.dto.MetaContentDto.Representation
import com.rarible.protocol.dto.NftCollectionMetaDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.nft.core.model.ContentMeta
import com.rarible.protocol.nft.core.model.TokenMeta
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.model.meta.EthImageProperties
import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.time.Instant

class NftCollectionMetaDtoConverterTest {

    private lateinit var converter: NftCollectionMetaDtoConverter

    @BeforeEach
    fun beforeEach() {
        converter = NftCollectionMetaDtoConverter
    }

    @Test
    fun `convert if content list is empty`() {
        assertThat(
            converter.convert(innerMeta)
        ).isEqualTo(expectedMeta)
    }

    @Test
    fun `convert if content list is not empty`() {
        assertThat(
            converter.convert(
                innerMeta.copy(
                    content = listOf(
                        EthMetaContent(
                            url = "other_url",
                            representation = Representation.PREVIEW,
                            fileName = "fileName",
                            properties = EthImageProperties(
                                mimeType = "gif",
                                size = 1000,
                                width = 300,
                                height = 400
                            )
                        )
                    )
                )
            )
        ).isEqualTo(
            expectedMeta.copy(
                content = listOf(
                    ImageContentDto(
                        fileName = "fileName",
                        url = "other_url",
                        representation = Representation.PREVIEW,
                        mimeType = "gif",
                        size = 1000,
                        width = 300,
                        height = 400
                    )
                )
            )
        )
    }

    @Test
    fun `convert if content list is empty and image is null`() {
        assertThat(
            converter.convert(
                innerMeta.copy(properties = innerMeta.properties.copy(image = null))
            )
        ).isEqualTo(
            expectedMeta.copy(
                image = null,
                content = null
            )
        )
    }

    companion object {
        private val createdAt = Instant.now()

        val innerMeta = TokenMeta(
            properties = TokenProperties(
                name = "name",
                description = "description",
                image = "http://test.com/abc_original",
                externalLink = "externalLink",
                feeRecipient = Address.ZERO(),
                sellerFeeBasisPoints = 0,
                createdAt = createdAt,
                tags = listOf("tag1", "tag2"),
                genres = listOf("genre1", "genre2"),
                language = "lang",
                rights = "rights",
                rightsUri = "rightsUri",
                externalUri = "externalUri"
            ),
            contentMeta = ContentMeta(
                type = "jpeg",
                width = 100,
                height = 200,
                size = 300
            ),
            content = emptyList()
        )

        val expectedMeta = NftCollectionMetaDto(
            name = "name",
            description = "description",
            createdAt = createdAt,
            tags = listOf("tag1", "tag2"),
            genres = listOf("genre1", "genre2"),
            language = "lang",
            rights = "rights",
            rightsUri = "rightsUri",
            externalUri = "externalUri",
            image = NftMediaDto(
                url = mapOf(
                    Pair("ORIGINAL", "http://test.com/abc_original")
                ),
                meta = mapOf(
                    Pair("ORIGINAL", NftMediaMetaDto("jpeg", 100, 200))
                )
            ),
            external_link = "externalLink",
            seller_fee_basis_points = 0,
            fee_recipient = Address.ZERO(),
            content = listOf(
                ImageContentDto(
                    fileName = null,
                    url = "http://test.com/abc_original",
                    representation = Representation.ORIGINAL,
                    mimeType = "jpeg",
                    size = 300,
                    width = 100,
                    height = 200
                )
            )
        )
    }
}
