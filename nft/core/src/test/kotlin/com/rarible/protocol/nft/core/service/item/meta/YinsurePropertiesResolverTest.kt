package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.YInsurePropertiesResolver
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@ItemMetaTest
class YinsurePropertiesResolverTest : BasePropertiesResolverTest() {

    private val yInsurePropertiesResolver: YInsurePropertiesResolver = YInsurePropertiesResolver(
        sender = createSender(),
        apiUrl = "http://localhost:8080"
    )

    @Test
    fun `yInsure resolver`() = runBlocking<Unit> {
        val properties = yInsurePropertiesResolver.resolve(
            ItemId(
                YInsurePropertiesResolver.YINSURE_ADDRESS,
                EthUInt256.of(48)
            )
        )
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "Balancer | 1 ETH \uD83D\uDD12 | 11/11/2020",
                image = "http://localhost:8080/image/yinsure/48.svg",
                description = "Covers Balancer Smart Contract risks worth 1 ETH. Policy is valid until Wed Nov 11 2020",
                imagePreview = null,
                imageBig = null,
                animationUrl = null,
                attributes = listOf(
                    ItemAttribute("token", "yinsure"),
                    ItemAttribute("currency", "ETH"),
                    ItemAttribute("amount", "1"),
                    ItemAttribute("expireTime", "1605086407"),
                    ItemAttribute("expirationTimestamp", "1605086407"),
                    ItemAttribute("validUntil", "11/11/2020"),
                    ItemAttribute("scAddressToCover", "0x9424b1412450d0f8fc2255faf6046b98213b76bd"),
                    ItemAttribute("iconUrl", "balancer.png"),
                    ItemAttribute("platform", "Balancer"),
                    ItemAttribute("startColor", "#575757"),
                    ItemAttribute("stopColor", "#000000")
                ),
                rawJsonContent = null
            )
        )
    }

    @Test
    fun `returns null for other address`() = runBlocking<Unit> {
        val properties = yInsurePropertiesResolver.resolve(
            ItemId(
                randomAddress(),
                EthUInt256.Companion.of(48)
            )
        )
        assertThat(properties).isNull()
    }
}
