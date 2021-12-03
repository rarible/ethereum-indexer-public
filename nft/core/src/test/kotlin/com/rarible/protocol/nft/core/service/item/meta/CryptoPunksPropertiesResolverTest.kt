package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.CryptoPunksMeta
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.CryptoPunksPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.CryptoPunksRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address

@ItemMetaTest
class CryptoPunksPropertiesResolverTest : BasePropertiesResolverTest() {

    private val cryptoPunksRepository: CryptoPunksRepository = mockk()
    private val cryptoPunksAddress: Address = Address.apply("0xb47e3cd837dDF8e4c57F05d70Ab865de6e193BBB")
    private val cryptoPunksPropertiesResolver = CryptoPunksPropertiesResolver(
        cryptoPunksRepository = cryptoPunksRepository,
        nftIndexerProperties = mockk {
            every { cryptoPunksContractAddress } returns cryptoPunksAddress.prefixed()
        }
    )

    @Test
    fun `crypto punks resolver`() = runBlocking<Unit> {
        val punkId = 33.toBigInteger()
        val image = "https://www.larvalabs.com/cryptopunks/cryptopunk33.png"
        val attributes = listOf(ItemAttribute("accessory", "Peak Spike"), ItemAttribute("type", "Male"))
        every { cryptoPunksRepository.findById(punkId) } returns CryptoPunksMeta(
            id = punkId,
            image = image,
            attributes = attributes
        ).toMono()
        val props = cryptoPunksPropertiesResolver.resolve(ItemId(cryptoPunksAddress, EthUInt256(punkId)))
        assertThat(props).isEqualTo(
            ItemProperties(
                name = "CryptoPunk #33",
                image = image,
                attributes = attributes,
                description = null,
                imagePreview = null,
                imageBig = null,
                animationUrl = null,
                rawJsonContent = null
            )
        )
    }

    @Test
    fun `returns null for other address`() = runBlocking<Unit> {
        val properties = cryptoPunksPropertiesResolver.resolve(
            ItemId(
                randomAddress(),
                EthUInt256.Companion.of(1)
            )
        )
        assertThat(properties).isNull()
    }
}
