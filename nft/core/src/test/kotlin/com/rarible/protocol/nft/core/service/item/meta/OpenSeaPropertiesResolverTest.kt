package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.OpenSeaPropertiesResolver
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import scalether.domain.Address

@ItemMetaTest
@EnabledIfSystemProperty(named = "RARIBLE_TESTS_OPENSEA_PROXY_URL", matches = ".+")
class OpenSeaPropertiesResolverTest : BasePropertiesResolverTest() {
    companion object {
        fun createOpenSeaPropertiesResolver() = OpenSeaPropertiesResolver(
            openseaUrl = "https://api.opensea.io/api/v1",
            openseaApiKey = "",
            readTimeout = 10000,
            connectTimeout = 3000,
            requestTimeout = 20000,
            proxyUrl = System.getProperty("RARIBLE_TESTS_OPENSEA_PROXY_URL")
        )
    }

    private val openSeaPropertiesResolver: OpenSeaPropertiesResolver = OpenSeaPropertiesResolver(
        openseaUrl = "https://api.opensea.io/api/v1",
        openseaApiKey = "",
        readTimeout = 10000,
        connectTimeout = 3000,
        requestTimeout = 20000,
        proxyUrl = System.getProperty("RARIBLE_TESTS_OPENSEA_PROXY_URL")
    )

    @Test
    fun `attribute with date time format`() = runBlocking<Unit> {
        val properties = openSeaPropertiesResolver.resolve(
            ItemId(
                Address.apply("0x302e848d900dc1ca0ff9274dbf3aa8f3ff5fcc44"),
                EthUInt256.of(12)
            )
        )!!
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "Gather Full Masternode - Tier 1 - 250000 GTH",
                description = "This NFT represents the ownership of Gather Network's Full Masternode, \$GTH masternode collateral is linked to the NFT. It is used as an access key to run masternode servers for Gather Cloud.",
                image = "https://ipfs.io/ipfs/QmWVcXnhhf9yo4q9C4QeADy5LL8UFBiCEqcjSfNMS9ugKj",
                imagePreview = "https://storage.opensea.io/files/f225c638e5f5b8621f844e81425c3c74.mp4",
                imageBig = "https://storage.opensea.io/files/f225c638e5f5b8621f844e81425c3c74.mp4",
                animationUrl = "https://storage.opensea.io/files/f225c638e5f5b8621f844e81425c3c74.mp4",
                attributes = listOf(
                    ItemAttribute("Batch Number", "1"),
                    ItemAttribute("GTH amount", "250000"),
                    ItemAttribute("Masternode Type", "Full"),
                    ItemAttribute("Active Since", "2020-12-08T00:00:00Z", type = "string", format = "date-time"),
                    ItemAttribute("Rewards", "Cloud Profits"),
                    ItemAttribute("Masternode Number", "12")
                ),
                rawJsonContent = null
            )
        )
    }


}
