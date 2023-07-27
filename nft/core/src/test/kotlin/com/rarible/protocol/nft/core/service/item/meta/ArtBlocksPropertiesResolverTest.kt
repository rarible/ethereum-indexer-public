package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.item.meta.descriptors.ArtBlocksPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigInteger

@ItemMetaTest
class ArtBlocksPropertiesResolverTest : BasePropertiesResolverTest() {

    private val token = Address.apply("0xa7d8d9ef8d8ce8992df33d8b8cf4aebabd5bd270")

    private val resolver = ArtBlocksPropertiesResolver(
        urlService,
        rawPropertiesProvider,
        tokenUriResolver
    )

    @BeforeEach
    fun mock() = mockTokenStandard(token, TokenStandard.ERC721)

    @Test
    fun `get properties`() = runBlocking<Unit> {
        val properties = resolver.resolve(
            ItemId(
                token,
                EthUInt256.of(BigInteger("114000001"))
            )
        )
        assertThat(properties?.copy(rawJsonContent = null)).isEqualTo(
            ItemProperties(
                name = "glitch crystal monsters #1",
                description = "A synthesis of over 777 days of generative artmaking, motion as survival, and coding as transformative ritual. Sky gardens of trustless techno-leviathans crystallize, entangle, and play between dimensions online and AFK, generating new forms altogether. I channel these speculative phenomena to highlight the fluid, transformative possibilities of structures perceived as rigid and immutable. Their intricate geometries move freely beyond limited social media compression that severely reduces the experience of digital art. These formations also embody choreographic elements from collaborative live coding dance performance.",
                attributes = listOf(
                    ItemAttribute("Sky", "Indigo"),
                    ItemAttribute("Monster", "Streamlined"),
                    ItemAttribute("Crystals", "medium"),
                    ItemAttribute("BlackandWhite", "off"),
                    ItemAttribute("project_id", "114"),
                    ItemAttribute("collection_name", "glitch crystal monsters by Alida Sun"),
                ),
                rawJsonContent = null,
                rights = "Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)",
                externalUri = "https://artblocks.io/collections/curated/projects/0xa7d8d9ef8d8ce8992df33d8b8cf4aebabd5bd270/114/tokens/114000001",
                tokenUri = "https://api.artblocks.io/token/114000001",
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "https://media-proxy.artblocks.io/0xa7d8d9ef8d8ce8992df33d8b8cf4aebabd5bd270/114000001.png",
                    videoOriginal = "https://generator.artblocks.io/0xa7d8d9ef8d8ce8992df33d8b8cf4aebabd5bd270/114000001"
                )
            )
        )
    }
}
