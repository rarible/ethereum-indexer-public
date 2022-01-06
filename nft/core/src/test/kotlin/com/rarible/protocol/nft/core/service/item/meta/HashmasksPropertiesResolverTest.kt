package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.HashmasksPropertiesResolver
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@ItemMetaTest
class HashmasksPropertiesResolverTest : BasePropertiesResolverTest() {

    private val hashmasksPropertiesResolver: HashmasksPropertiesResolver = HashmasksPropertiesResolver(
        sender = createSender(),
        ipfsService = IpfsService()
    )

    @Test
    fun `hashmasks resolver`() = runBlocking<Unit> {
        val properties = hashmasksPropertiesResolver.resolve(
            ItemId(
                HashmasksPropertiesResolver.HASH_MASKS_ADDRESS,
                EthUInt256.of(7646)
            )
        )
        assertThat(properties).isEqualTo(
            ItemProperties(
                name = "African Warrior",
                description = "Hashmasks is a living digital art collectible created by over 70 artists globally. It is a collection of 16,384 unique digital portraits. Brought to you by Suum Cuique Labs from Zug, Switzerland.",
                image = "https://rarible.mypinata.cloud/ipfs/QmZ4mhghewEViUEDgYk4pHjQwUByfaBh45eqbYKmwxHJBh",
                imageBig = null,
                imagePreview = null,
                animationUrl = null,
                attributes = listOf(
                    ItemAttribute("character", "Male"),
                    ItemAttribute("mask", "Doodle"),
                    ItemAttribute("eyeColor", "Green"),
                    ItemAttribute("skinColor", "Dark"),
                    ItemAttribute("item", "No Item")
                ),
                rawJsonContent = null
            )
        )
    }

    @Test
    fun `returns null for other address`() = runBlocking<Unit> {
        val properties = hashmasksPropertiesResolver.resolve(
            ItemId(
                randomAddress(),
                EthUInt256.Companion.of(9076)
            )
        )
        assertThat(properties).isNull()
    }
}
