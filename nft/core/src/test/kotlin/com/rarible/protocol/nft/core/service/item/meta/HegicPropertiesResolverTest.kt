package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.HegicPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.HegicPropertiesResolver.Companion.HEGIC_ADDRESS
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

@ItemMetaTest
class HegicPropertiesResolverTest : BasePropertiesResolverTest() {

    private val hegicPropertiesResolver: HegicPropertiesResolver = HegicPropertiesResolver(
        sender = createSender(),
        apiUrl = "http://localhost:8080"
    )

    @Test
    fun `hegic resolver`() = runBlocking<Unit> {
        // TODO[meta]: hegic contract fails with math overflow on this item.
        Assumptions.assumeFalse(true)
        val properties = hegicPropertiesResolver.resolve(
            ItemId(
                HEGIC_ADDRESS,
                EthUInt256.of(317)
            )
        )
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "Kurisu Makise",
                description = "Waifusion is a digital Waifu collection. There are 16,384 guaranteed-unique Waifusion NFTs. They’re just like you; a beautiful work of art, but 2-D and therefore, superior, Anon-kun.",
                image = "https://ipfs.io/ipfs/QmQuzMGqHxSXugCUvWQjDCDHWhiGB75usKbrZk6Ec6pFvw/1.png",
                imageBig = null,
                imagePreview = null,
                animationUrl = null,
                attributes = listOf(
                    ItemAttribute("token", "waifusion"),
                    ItemAttribute("owner", "0x574a782a00dd152d98ff85104f723575d870698e")
                ),
                rawJsonContent = null
            )
        )
    }

    @Test
    fun `returns null for other address`() = runBlocking<Unit> {
        val properties = hegicPropertiesResolver.resolve(
            ItemId(
                randomAddress(),
                EthUInt256.Companion.of(317)
            )
        )
        assertThat(properties).isNull()
    }

}
