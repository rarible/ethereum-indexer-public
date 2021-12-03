package com.rarible.protocol.nft.core.service.item.meta

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.LootPropertiesResolver
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@ItemMetaTest
class LootPropertiesResolverTest : BasePropertiesResolverTest() {

    private val lootPropertiesResolver = LootPropertiesResolver(
        sender = createSender(),
        mapper = jacksonObjectMapper(),
        ipfsService = IpfsService()
    )

    @Test
    fun `loot resolver`() = runBlocking<Unit> {
        val properties = lootPropertiesResolver.resolve(
            ItemId(
                LootPropertiesResolver.LOOT_ADDRESS,
                EthUInt256.Companion.of(10)
            )
        )
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "Bag #10",
                description = "Loot is randomized adventurer gear generated and stored on chain. Stats, images, and other functionality are intentionally omitted for others to interpret. Feel free to use Loot in any way you want.",
                image = "${IpfsService.RARIBLE_IPFS}/ipfs/QmaA5x5Hj9gQRaTLbpVzv3ruvSjF2T9FMLkPqtCQzXYJbU",
                animationUrl = "${IpfsService.RARIBLE_IPFS}/ipfs/QmaA5x5Hj9gQRaTLbpVzv3ruvSjF2T9FMLkPqtCQzXYJbU",
                imageBig = null,
                imagePreview = null,
                attributes = listOf(
                    ItemAttribute("chest", "Robe"),
                    ItemAttribute("foot", "Holy Greaves"),
                    ItemAttribute("hand", "Wool Gloves"),
                    ItemAttribute("head", "Divine Hood"),
                    ItemAttribute("neck", "\"Havoc Sun\" Amulet of Reflection"),
                    ItemAttribute("ring", "Platinum Ring"),
                    ItemAttribute("waist", "Studded Leather Belt"),
                    ItemAttribute("weapon", "Maul")
                ),
                rawJsonContent = null
            )
        )
    }

    @Test
    fun `returns null for other address`() = runBlocking<Unit> {
        val properties = lootPropertiesResolver.resolve(
            ItemId(
                randomAddress(),
                EthUInt256.Companion.of(10)
            )
        )
        assertThat(properties).isNull()
    }
}
