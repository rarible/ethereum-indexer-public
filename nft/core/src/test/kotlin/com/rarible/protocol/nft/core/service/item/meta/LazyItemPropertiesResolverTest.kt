package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.randomItemProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.service.item.meta.descriptors.LazyItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RariblePropertiesResolver
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address

class LazyItemPropertiesResolverTest : BasePropertiesResolverTest() {

    private val lazyNftItemHistoryRepository = mockk<LazyNftItemHistoryRepository>()
    private val rariblePropertiesResolverMock = mockk<RariblePropertiesResolver>()
    private val lazyItemPropertiesResolver = LazyItemPropertiesResolver(
        rariblePropertiesResolver = rariblePropertiesResolverMock,
        lazyNftItemHistoryRepository = lazyNftItemHistoryRepository,
        urlParser = urlParser,
        ipfsProperties = NftIndexerProperties.IpfsProperties(
            ipfsGateway = "http://privateipfs.com",
            ipfsPublicGateway = "http://privateipfs.com",
            ipfsLazyGateway = "http://lazygatewway.com"
        )
    )
    private val ipfsPath = "QmbpJhWFiwzNu7MebvKG3hrYiyWmSiz5dTUYMQLXsjT9vw"

    @Test
    fun `resolved - ok, regular url`() = runBlocking<Unit> {
        val token = randomAddress()
        val tokenId = EthUInt256(randomBigInt())
        val itemId = ItemId(token, tokenId)
        val tokenUri = "http://something.com/123"

        mockLazyMint(token, tokenId, tokenUri)

        val itemProperties = randomItemProperties()
        coEvery { rariblePropertiesResolverMock.resolveByTokenUri(itemId, tokenUri) } returns itemProperties

        val properties = lazyItemPropertiesResolver.resolve(itemId)
        assertThat(properties).isEqualTo(itemProperties)
    }

    @Test
    fun `resolved - ok, lazy ipfs url`() = runBlocking<Unit> {
        val token = randomAddress()
        val tokenId = EthUInt256(randomBigInt())
        val itemId = ItemId(token, tokenId)
        val tokenUri = "/ipfs/$ipfsPath"

        mockLazyMint(token, tokenId, tokenUri)

        val itemProperties = randomItemProperties()
        val lazyTokenUri = "http://lazygatewway.com/ipfs/$ipfsPath"
        coEvery { rariblePropertiesResolverMock.resolveByTokenUri(itemId, lazyTokenUri) } returns itemProperties

        val properties = lazyItemPropertiesResolver.resolve(itemId)
        assertThat(properties).isEqualTo(itemProperties)
    }

    private fun mockLazyMint(token: Address, tokenId: EthUInt256, tokenUri: String) {
        every {
            lazyNftItemHistoryRepository.find(any(), any(), any())
        } returns listOf(
            ItemLazyMint(
                token = token,
                tokenId = tokenId,
                value = EthUInt256.ONE,
                date = nowMillis(),
                uri = tokenUri,
                standard = TokenStandard.ERC721,
                creators = listOf(Part(randomAddress(), 10000)),
                royalties = emptyList(),
                signatures = emptyList()
            )
        ).toFlux()
    }
}
