package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.CryptoKittiesPropertiesResolver
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@ItemMetaTest
class CryptoKittiesPropertiesResolverTest : BasePropertiesResolverTest() {

    private val cryptoKittiesPropertiesResolver = CryptoKittiesPropertiesResolver()

    @Test
    fun `cryptoKitties resolver`() = runBlocking<Unit> {
        val properties = cryptoKittiesPropertiesResolver.resolve(
            ItemId(
                CryptoKittiesPropertiesResolver.CRYPTO_KITTIES_ADDRESS,
                EthUInt256.Companion.of(1001)
            )
        )
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "TheFirst",
                description = "Hey cutie! I'm TheFirst. In high school, I was voted most likely to work at NASA. When my owner isn't watching, I steal their oversized sweaters and use them for litter paper. I'm not sorry. I think you'll love me beclaws I have cattitude.",
                image = "https://img.cn.cryptokitties.co/0x06012c8cf97bead5deae237070f9587f8e7a266d/1001.svg",
                imagePreview = null,
                imageBig = null,
                animationUrl = null,
                attributes = listOf(
                    ItemAttribute("colorprimary", "shadowgrey"),
                    ItemAttribute("coloreyes", "mintgreen"),
                    ItemAttribute("body", "ragamuffin"),
                    ItemAttribute("colorsecondary", "swampgreen"),
                    ItemAttribute("mouth", "happygokitty"),
                    ItemAttribute("pattern", "luckystripe"),
                    ItemAttribute("eyes", "crazy"),
                    ItemAttribute("colortertiary", "granitegrey"),
                    ItemAttribute("secret", "se4"),
                    ItemAttribute("purrstige", "pu9")
                ),
                rawJsonContent = null
            )
        )
    }

    @Test
    fun `returns null for other address`() = runBlocking<Unit> {
        val properties = cryptoKittiesPropertiesResolver.resolve(
            ItemId(
                randomAddress(),
                EthUInt256.Companion.of(1001)
            )
        )
        assertThat(properties).isNull()
    }
}
