package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.AlchemistCruciblePropertiesResolver
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigInteger

@ItemMetaTest
class AlchemistCruciblePropertiesResolverTest : BasePropertiesResolverTest() {

    private val resolver = AlchemistCruciblePropertiesResolver(urlService, rawPropertiesProvider)

    @Test
    fun `get properties`() = runBlocking<Unit> {
        val properties = resolver.resolve(
            ItemId(
                AlchemistCruciblePropertiesResolver.ALCHEMIST_CRUCIBLE_V1_ADDRESS,
                EthUInt256.of(BigInteger("96743992780796070602971068741790707060398384883"))
            )
        )
        Assertions.assertThat(properties).isEqualTo(
            ItemProperties(
                name = "Basic Crucible (Unrevealed)",
                description = "## Crucible V1 NFT\n\nAddress: 0x10f22692797bc5b622ecd1113455b0e5326466f3\n\n[View on Explorer](https://etherscan.io/address/0x10f22692797bc5b622ecd1113455b0e5326466f3)\n\n[View contents on Crucible explorer](https://crucible.wtf/explore/0x10f22692797bc5b622ecd1113455b0e5326466f3?network=1)",
                image = "https://thumbnail.crucible.wtf/?id=0x10f22692797bc5b622ecd1113455b0e5326466f3&chainId=1",
                imagePreview = null,
                imageBig = null,
                animationUrl = "https://crucible.wtf/nft-scene/0x10f22692797bc5b622ecd1113455b0e5326466f3?network=1",
                attributes = listOf(
                    ItemAttribute("Mint date", "2022-05-30T15:25:57Z", "string", "date-time"),
                    ItemAttribute("Mint number", "9888"),
                ),
                rawJsonContent = "{\"name\":\"Basic Crucible (Unrevealed)\",\"description\":\"## Crucible V1 NFT\\n\\nAddress: 0x10f22692797bc5b622ecd1113455b0e5326466f3\\n\\n[View on Explorer](https://etherscan.io/address/0x10f22692797bc5b622ecd1113455b0e5326466f3)\\n\\n[View contents on Crucible explorer](https://crucible.wtf/explore/0x10f22692797bc5b622ecd1113455b0e5326466f3?network=1)\",\"image\":\"https://thumbnail.crucible.wtf/?id=0x10f22692797bc5b622ecd1113455b0e5326466f3&chainId=1\",\"animation_url\":\"https://crucible.wtf/nft-scene/0x10f22692797bc5b622ecd1113455b0e5326466f3?network=1\",\"external_url\":\"https://crucible.wtf/explore/0x10f22692797bc5b622ecd1113455b0e5326466f3?network=1\",\"seller_fee_basis_points\":250,\"fee_recipient\":\"0xDa854b22F866C5a2B566dE7F1D1F6F116Db5a409\",\"attributes\":[{\"display_type\":\"date\",\"trait_type\":\"Mint date\",\"value\":1653924357},{\"display_type\":\"number\",\"trait_type\":\"Mint number\",\"value\":9888}]}"
            )
        )
    }
}
