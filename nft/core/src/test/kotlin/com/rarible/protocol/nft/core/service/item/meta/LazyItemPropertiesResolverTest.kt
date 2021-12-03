package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
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

@ItemMetaTest
class LazyItemPropertiesResolverTest : BasePropertiesResolverTest() {

    private val lazyNftItemHistoryRepository = mockk<LazyNftItemHistoryRepository>()
    private val rariblePropertiesResolver = mockk<RariblePropertiesResolver>()
    private val lazyItemPropertiesResolver = LazyItemPropertiesResolver(
        rariblePropertiesResolver,
        lazyNftItemHistoryRepository
    )

    @Test
    fun `lazy item properties resolver`() = runBlocking<Unit> {
        val token = randomAddress()
        val tokenId = EthUInt256(randomBigInt())
        val tokenUri = "lazyTokenUri"
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

        val itemProperties = randomItemProperties()
        val itemId = ItemId(token, tokenId)
        coEvery { rariblePropertiesResolver.resolveByTokenUri(itemId, tokenUri) } returns itemProperties
        val properties = lazyItemPropertiesResolver.resolve(itemId)
        assertThat(properties).isEqualTo(itemProperties)
    }
}
