package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.item.meta.descriptors.PxlvrPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

@ItemMetaTest
class PxlvrPropertiesResolverTest : BasePropertiesResolverTest() {

    private val resolver = PxlvrPropertiesResolver(
        urlService,
        rawPropertiesProvider,
        tokenUriResolver
    )

    @Test
    fun `get properties`() = runBlocking<Unit> {
        val token = PxlvrPropertiesResolver.PXLVR_ADDRESS
        val tokenId = BigInteger("6")
        val itemId = ItemId(token, tokenId)

        mockTokenStandard(token, TokenStandard.ERC721)
        val properties = resolver.resolve(itemId)

        assertThat(properties?.copy(rawJsonContent = null)).isEqualTo(
            ItemProperties(
                name = "pxlj03.5",
                description = "VR p4!nt3d p!x3l w0rk.\n\n1080x1920px. 36 s3c0nd v!d30.\n\nMus!c by Lenny",
                attributes = emptyList(),
                tokenUri = "https://ipfs.pixura.io/ipfs/QmR5KNBDtYXDb5PeHkZo9ZcdKjvtz9NdaW6YRR8vwzvcaA/metadata.json",
                rawJsonContent = null,
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "https://ipfs.pixura.io/ipfs/QmUqwAtCgsXcVKCMsQ7UiNC7685QUi22WpQ6Wgxr1YtmC4/pxlj03.5.mp4"
                )
            )
        )
    }
}
