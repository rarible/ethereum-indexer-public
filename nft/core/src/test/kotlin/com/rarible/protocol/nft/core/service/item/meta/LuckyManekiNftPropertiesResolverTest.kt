package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.item.meta.descriptors.LuckyManekiNftPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import scalether.domain.Address

@ItemMetaTest
@EnabledIfSystemProperty(named = "RARIBLE_TESTS_OPENSEA_PROXY_URL", matches = ".+")
class LuckyManekiNftPropertiesResolverTest : BasePropertiesResolverTest() {
    private val luckyManekiNftPropertiesResolver = LuckyManekiNftPropertiesResolver(
        externalHttpClient = externalHttpClient
    )

    @Test
    fun luckyManekiNft() = runBlocking<Unit> {
        val address = Address.apply("0x14f03368b43e3a3d27d45f84fabd61cc07ea5da3")
        mockTokenStandard(address, TokenStandard.ERC721)
        val properties = luckyManekiNftPropertiesResolver.resolve(
            ItemId(
                address,
                EthUInt256.of(9163)
            )
        )
        val expectedProperties = ItemProperties(
            name = "Lucky Maneki #9163",
            description = null,
            attributes = listOf(
                ItemAttribute("Environment", "Regional"),
                ItemAttribute("Environment Subtype", "Latin American"),
                ItemAttribute("Environment Implicit", "Vinicunca Green"),
                ItemAttribute("Body Type", "Freaky"),
                ItemAttribute("Body Subtype", "Hypnocat"),
                ItemAttribute("Head Type", "Cinematic"),
                ItemAttribute("Head Subtype", "Alien"),
                ItemAttribute("Head Implicit", "Alien"),
                ItemAttribute("Lower Paw Type", "Empty"),
                ItemAttribute("Upper Paw Type", "Empty")
            ),
            content = ContentBuilder.getItemMetaContent(
                imageOriginal = "https://lucky-maneki.s3.amazonaws.com/originals/ee1aa0505c2fdd4f.png"
            ),
            rawJsonContent = """{
  "name": "Lucky Maneki #9163",
  "image": "https://lucky-maneki.s3.amazonaws.com/originals/ee1aa0505c2fdd4f.png",
  "external_url": "https://luckymaneki.com/gallery/9163",
  "attributes": [
    {
      "trait_type": "Environment",
      "value": "Regional"
    },
    {
      "trait_type": "Environment Subtype",
      "value": "Latin American"
    },
    {
      "trait_type": "Environment Implicit",
      "value": "Vinicunca Green"
    },
    {
      "trait_type": "Body Type",
      "value": "Freaky"
    },
    {
      "trait_type": "Body Subtype",
      "value": "Hypnocat"
    },
    {
      "trait_type": "Head Type",
      "value": "Cinematic"
    },
    {
      "trait_type": "Head Subtype",
      "value": "Alien"
    },
    {
      "trait_type": "Head Implicit",
      "value": "Alien"
    },
    {
      "trait_type": "Lower Paw Type",
      "value": "Empty"
    },
    {
      "trait_type": "Upper Paw Type",
      "value": "Empty"
    }
  ]
}"""
        )
        assertThat(properties).isEqualTo(expectedProperties)
    }
}
