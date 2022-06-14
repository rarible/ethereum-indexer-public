package com.rarible.protocol.nft.core.converters.dto

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.MetaContentDto.Representation
import com.rarible.protocol.nft.core.data.randomTokenProperties
import com.rarible.protocol.nft.core.model.ContentMeta
import com.rarible.protocol.nft.core.model.TokenMeta
import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NftCollectionMetaDtoConverterTest {

    private val converter = NftCollectionMetaDtoConverter

    @Test
    fun `convert token`() {
        // We don't need to check content conversion here, we should have separate test for it
        val properties = randomTokenProperties().copy(
            content = listOf(EthMetaContent(randomString(), Representation.ORIGINAL))
        )
        val contentMeta = ContentMeta("text/html", 100, 200, 300)
        val meta = converter.convert(TokenMeta(properties, contentMeta))

        assertThat(meta.name).isEqualTo(properties.name)
        assertThat(meta.description).isEqualTo(properties.description)
        assertThat(meta.feeRecipient).isEqualTo(properties.feeRecipient)
        assertThat(meta.createdAt).isEqualTo(properties.createdAt)

        assertThat(meta.tags).isEqualTo(properties.tags)
        assertThat(meta.genres).isEqualTo(properties.genres)
        assertThat(meta.rights).isEqualTo(properties.rights)
        assertThat(meta.rightsUri).isEqualTo(properties.rightsUri)
        assertThat(meta.externalUri).isEqualTo(properties.externalUri)
        assertThat(meta.originalMetaUri).isEqualTo(properties.tokenUri)
        assertThat(meta.content).hasSize(properties.content.size)

        assertThat(meta.seller_fee_basis_points).isEqualTo(properties.sellerFeeBasisPoints)
        assertThat(meta.external_link).isEqualTo(properties.externalUri)
        assertThat(meta.fee_recipient).isEqualTo(properties.feeRecipient)
    }
}
