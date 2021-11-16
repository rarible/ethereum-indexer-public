@file:Suppress("SpellCheckingInspection")

package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.item.meta.descriptors.OpenSeaPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RariblePropertiesResolver
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import scalether.domain.Address

@ItemMetaTest
class ItemPropertiesServiceMainnetTest : BasePropertiesResolverTest() {
    private val rariblePropertiesResolver = RariblePropertiesResolver(
        sender = createSender(),
        tokenRepository = tokenRepository,
        ipfsService = IpfsService(),
        requestTimeout = 20000
    )
    private val openSeaPropertiesResolver = OpenSeaPropertiesResolver(
        openseaUrl = "https://api.opensea.io/api/v1",
        openseaApiKey = "",
        readTimeout = 10000,
        connectTimeout = 3000,
        requestTimeout = 20000,
        proxyUrl = System.getProperty("RARIBLE_TESTS_OPENSEA_PROXY_URL")
    )
    private val service = ItemPropertiesService(
        itemPropertiesResolverProvider = mockk {
            every { orderedResolvers } returns listOf(rariblePropertiesResolver)
            every { openSeaResolver } returns openSeaPropertiesResolver
        },
        ipfsService = IpfsService(),
        cacheTimeout = 10000,
        cacheService = null
    )

    @Test
    fun `compare rarible and opensea resolvers`() = runBlocking<Unit> {
        val raribleWebUrlPrefix = "https://rarible.com"
        val openSeaWebUrlPrefix = "https://opensea.io/assets"
        val items = listOf(
            // https://rarible.com/token/0xbc4ca0eda7647a8ab7c2061c2e118a18a936f13d:9187?tab=details
            ItemId(
                Address.apply("0xbc4ca0eda7647a8ab7c2061c2e118a18a936f13d"),
                EthUInt256.of(9187)
            )
        )
        for (itemId in items) {
            mockTokenStandard(itemId.token, TokenStandard.ERC721)
            println("Comparing ${itemId.decimalStringValue}")
            println("  Rarible Web ${raribleWebUrlPrefix}/${itemId.token}:${itemId.tokenId.value}")
            println("  OpenSea Web ${openSeaWebUrlPrefix}/${itemId.token}/${itemId.tokenId.value}")
            val raribleProperties = rariblePropertiesResolver.resolve(itemId)
            val openSeaProperties = openSeaPropertiesResolver.resolve(itemId)
            println("  Rarible: $raribleProperties")
            println("  OpenSea: $openSeaProperties")
            val resolved = service.resolve(itemId)
            println("  Resolved: $resolved")
        }
    }
}
