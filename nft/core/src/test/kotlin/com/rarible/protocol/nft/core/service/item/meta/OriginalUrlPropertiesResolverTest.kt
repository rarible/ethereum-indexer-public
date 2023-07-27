package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.item.meta.descriptors.OriginalUrlPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

@ItemMetaTest
class OriginalUrlPropertiesResolverTest : BasePropertiesResolverTest() {

    private val originalUrlPropertiesResolver = OriginalUrlPropertiesResolver(
        urlService,
        rawPropertiesProvider,
        tokenUriResolver
    )

    @Test
    fun `crypto cube resolver`() = runBlocking<Unit> {
        val token = OriginalUrlPropertiesResolver.CRYPTO_CUBE_ADDRESS
        val tokenId = BigInteger("201")
        val itemId = ItemId(token, tokenId)

        mockTokenStandard(token, TokenStandard.ERC721)
        val properties = originalUrlPropertiesResolver.resolve(itemId)

        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "CryptoCube #171",
                description = "It's all about cubes",
                attributes = listOf(
                    ItemAttribute("Landmark", "N"),
                    ItemAttribute("Color", "Fusion"),
                    ItemAttribute("Volume", "Stack")
                ),
                externalUri = "https://cryptocubes.io/collection/171",
                tokenUri = "https://cryptocubes.io/api/v1/ipfs/QmbKP6tTL6getrPaoP2j5XPAj3Mgy1LTnZGRfFBYP3N1My",
                rawJsonContent = "{\"name\":\"CryptoCube #171\",\"description\":\"It's all about cubes\",\"image\":\"https://cdn.cryptocubes.io/tokens/171.jpg\",\"animation_url\":\"https://cdn.cryptocubes.io/tokens/171.html\",\"external_url\":\"https://cryptocubes.io/collection/171\",\"attributes\":[{\"trait_type\":\"Landmark\",\"value\":\"N\"},{\"trait_type\":\"Color\",\"value\":\"Fusion\"},{\"trait_type\":\"Volume\",\"value\":\"Stack\"}]}",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "https://cdn.cryptocubes.io/tokens/171.jpg",
                    videoOriginal = "https://cdn.cryptocubes.io/tokens/171.html",
                )
            )
        )
    }
}
