package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.PegaxyPropertiesResolver
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigInteger

@ItemMetaTest
class PegaxyPropertiesResolverTest : BasePropertiesResolverTest() {

    private val resolver = PegaxyPropertiesResolver(externalHttpClient)

    @Test
    fun `get properties`() = runBlocking<Unit> {
        val properties = resolver.resolve(
            ItemId(
                PegaxyPropertiesResolver.PEGAXY_ADDRESS,
                EthUInt256.of(BigInteger("469595"))
            )
        )
        Assertions.assertThat(properties?.copy(rawJsonContent = null)).isEqualTo(
            ItemProperties(
                name = "Reloaded | XLR",
                description = null,
                image = "https://cdn.pegaxy.io/data/pega/1648350168034",
                imagePreview = null,
                imageBig = null,
                animationUrl = null,
                attributes = listOf(
                    ItemAttribute("Gender", "Male"),
                    ItemAttribute("Blood Line", "Hoz"),
                    ItemAttribute("Breed Type", "Pacer"),
                ),
                rawJsonContent = null
            )
        )
    }
}
