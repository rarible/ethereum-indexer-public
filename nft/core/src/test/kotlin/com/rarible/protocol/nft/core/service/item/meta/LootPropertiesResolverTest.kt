package com.rarible.protocol.nft.core.service.item.meta

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.LootPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@ItemMetaTest
class LootPropertiesResolverTest : BasePropertiesResolverTest() {

    private val lootPropertiesResolver = LootPropertiesResolver(
        sender = sender,
        mapper = jacksonObjectMapper()
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
                rawJsonContent = null,
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "<svg xmlns=\"http://www.w3.org/2000/svg\" preserveAspectRatio=\"xMinYMin meet\" viewBox=\"0 0 350 350\"><style>.base { fill: white; font-family: serif; font-size: 14px; }</style><rect width=\"100%\" height=\"100%\" fill=\"black\" /><text x=\"10\" y=\"20\" class=\"base\">Maul</text><text x=\"10\" y=\"40\" class=\"base\">Robe</text><text x=\"10\" y=\"60\" class=\"base\">Divine Hood</text><text x=\"10\" y=\"80\" class=\"base\">Studded Leather Belt</text><text x=\"10\" y=\"100\" class=\"base\">Holy Greaves</text><text x=\"10\" y=\"120\" class=\"base\">Wool Gloves</text><text x=\"10\" y=\"140\" class=\"base\">\"Havoc Sun\" Amulet of Reflection</text><text x=\"10\" y=\"160\" class=\"base\">Platinum Ring</text></svg>",
                    videoOriginal = "<svg xmlns=\"http://www.w3.org/2000/svg\" preserveAspectRatio=\"xMinYMin meet\" viewBox=\"0 0 350 350\"><style>.base { fill: white; font-family: serif; font-size: 14px; }</style><rect width=\"100%\" height=\"100%\" fill=\"black\" /><text x=\"10\" y=\"20\" class=\"base\">Maul</text><text x=\"10\" y=\"40\" class=\"base\">Robe</text><text x=\"10\" y=\"60\" class=\"base\">Divine Hood</text><text x=\"10\" y=\"80\" class=\"base\">Studded Leather Belt</text><text x=\"10\" y=\"100\" class=\"base\">Holy Greaves</text><text x=\"10\" y=\"120\" class=\"base\">Wool Gloves</text><text x=\"10\" y=\"140\" class=\"base\">\"Havoc Sun\" Amulet of Reflection</text><text x=\"10\" y=\"160\" class=\"base\">Platinum Ring</text></svg>",
                )
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
