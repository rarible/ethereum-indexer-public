package com.rarible.protocol.nft.core.converters.dto

import com.rarible.core.test.data.randomString
import com.rarible.protocol.nft.core.data.randomTokenProperties
import com.rarible.protocol.nft.core.model.TokenMeta
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthCollectionMetaDtoConverterTest {

    @Test
    fun `convert token`() {
        // We don't need to check content conversion here, we should have separate test for it
        val properties = randomTokenProperties().copy(
            content = ContentBuilder.getTokenMetaContent(randomString())
        )
        val meta = EthCollectionMetaDtoConverter.convert(TokenMeta(properties))

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
        assertThat(meta.content).hasSize(properties.content.asList().size)
    }
}
