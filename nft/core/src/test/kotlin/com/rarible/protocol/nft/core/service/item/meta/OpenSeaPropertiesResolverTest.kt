package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.OpenSeaPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import scalether.domain.Address

@ItemMetaTest
@EnabledIfSystemProperty(named = "RARIBLE_TESTS_OPENSEA_PROXY_URL", matches = ".+")
class OpenSeaPropertiesResolverTest : BasePropertiesResolverTest() {
    private val coreProperties = mockk<NftIndexerProperties> {
        every { blockchain } returns Blockchain.POLYGON
        every { opensea } returns NftIndexerProperties.OpenseaProperties().copy(url = openseaUrl)
    }
    private val openSeaPropertiesResolver = OpenSeaPropertiesResolver(
        externalHttpClient = externalHttpClient,
        properties = coreProperties
    )
    private val polygonOpenSeaPropertiesResolver = OpenSeaPropertiesResolver(
        externalHttpClient = externalHttpClient,
        properties = coreProperties
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
                attributes = listOf(
                    ItemAttribute("Batch Number", "1"),
                    ItemAttribute("GTH amount", "250000"),
                    ItemAttribute("Masternode Type", "Full"),
                    ItemAttribute("Active Since", "2020-12-08T00:00:00Z", type = "string", format = "date-time"),
                    ItemAttribute("Rewards", "Cloud Profits"),
                    ItemAttribute("Masternode Number", "12")
                ),
                rawJsonContent = null,
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "https://ipfs.io/ipfs/QmWVcXnhhf9yo4q9C4QeADy5LL8UFBiCEqcjSfNMS9ugKj",
                    imageBig = "https://openseauserdata.com/files/f225c638e5f5b8621f844e81425c3c74.mp4",
                    imagePreview = "https://openseauserdata.com/files/f225c638e5f5b8621f844e81425c3c74.mp4",
                    videoOriginal = "https://openseauserdata.com/files/f225c638e5f5b8621f844e81425c3c74.mp4"
                )
            )
        )
    }

    @Test
    fun `polygon meta resolve`() = runBlocking<Unit> {
        val properties = polygonOpenSeaPropertiesResolver.resolve(
            ItemId(
                Address.apply("0xb5dde149ff514dedea40914bd9852bbacb5b602b"),
                EthUInt256.of(50)
            )
        )!!
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "Wolverine - (1990 Marvel PSA 9)",
                description = "Wolverine - Wolverine is one of the most powerful, fierceful leaders of the X-Men. He is most commonly known for his beastly, retractable claws, his super strength, and his superb durability.\n" +
                        "\n" +
                        "The 1990 Marvel release from Impel is one of the most iconic trading card sets of all time. The set was produced in massive (but unknown) quantity and distributed to countless retailers.  Therefore, many collectors from the early 90s era have experienced ripping these packs. \n" +
                        "\n" +
                        "The set featured a 162 card setlist and it set the tone for marvel releases that would follow with vibrant illustrations and deep character write ups. \n" +
                        "\n" +
                        "Finding these cards isn’t difficult but the set is incredibly condition sensitive and high grade copies are rare. The cards are prone to significant edge chipping and a vast majority of the cards are terribly off center.  Well centered copies are easy to spot because of the white border design and this increases the desirability and overall appeal of well centered examples. \n" +
                        "\n" +
                        "The 1990 Impel Marvel Universe set is a perfect addition to the Ledger of Things.\n" +
                        "\n" +
                        "Collect all of the LOTTs (all of the tokens) for this thing (that aren't \"lost\" in an inactive wallet*) and you can choose to have a 1-of-1 NFT made to signify your unified ownership or you may choose to burn the LOTTs and remove the physical card from the LOT :(  Choose Wisely.\n" +
                        "\n" +
                        "An \"inactive wallet\" is any wallet that hasn't had an \"outbound\" token transfer (NFTs or coins/currency) in the past 5 years.  Outbound transfers only are used to specifically exclude potential \"air drops\" going into a wallet from causing an inactive wallet to otherwise appear active.",
                attributes = listOf(
                    ItemAttribute("Collectible Type", "Trading Card"),
                    ItemAttribute("Category", "Entertainment"),
                ),
                rawJsonContent = null,
                content = ContentBuilder.getItemMetaContent(
                    imageOriginal = "https://lh3.googleusercontent.com/ZhYAE4l86Xx3kNHzBLvervNxocK_QD0yr-MXcMGeWCN53_jdYF9Gsgu-j58q8bPEbdF-hTtud7C1GsRO8G4Y1Bvd9WaWeEpk9vQPRmQ"
                )
            )
        )
    }

    @Test
    fun `empty meta filtered`() = runBlocking<Unit> {
        val properties = openSeaPropertiesResolver.resolve(
            ItemId(
                Address.apply("0x8f4cac6469790a71514cda50e6d8fac3cdb1aa98"),
                EthUInt256.of(2120)
            )
        )

        assertThat(properties).isNull()
    }
}
