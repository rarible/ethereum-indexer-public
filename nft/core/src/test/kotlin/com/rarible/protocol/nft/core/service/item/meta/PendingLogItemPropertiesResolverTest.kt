package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.core.common.justOrEmpty
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.data.randomItemProperties
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.meta.descriptors.PendingLogItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RariblePropertiesResolver
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PendingLogItemPropertiesResolverTest : BasePropertiesResolverTest() {

    private val rariblePropertiesResolver = mockk<RariblePropertiesResolver>()
    private val itemRepository = mockk<ItemRepository>()
    private val pendingLogItemPropertiesResolver = PendingLogItemPropertiesResolver(
        itemRepository,
        rariblePropertiesResolver
    )

    @Test
    fun `resolve properties for a pending minting item`() = runBlocking<Unit> {
        val token = randomAddress()
        val tokenId = EthUInt256(randomBigInt())
        val itemId = ItemId(token, tokenId)

        val tokenUri = "lazyTokenUri"
        val itemProperties = randomItemProperties()
        coEvery { rariblePropertiesResolver.resolveByTokenUri(itemId, tokenUri) } returns itemProperties

        val item = Item(
            token = itemId.token,
            tokenId = itemId.tokenId,
            date = nowMillis(),
            supply = EthUInt256.ZERO,
            royalties = emptyList(),
            revertableEvents = listOf(
                createRandomMintItemEvent().copy(
                    tokenUri = tokenUri
                ).let {
                    it.copy(log = it.log.copy(status = EthereumLogStatus.PENDING))
                }
            )
        )
        every { itemRepository.findById(itemId) } returns item.justOrEmpty()

        val resolved = pendingLogItemPropertiesResolver.resolve(itemId)
        assertThat(resolved).isEqualTo(itemProperties)

        // Then the item becomes confirmed and the resolver must return null.
        clearMocks(itemRepository)

        every { itemRepository.findById(itemId) } returns item.copy(revertableEvents = emptyList()).justOrEmpty()
        assertThat(pendingLogItemPropertiesResolver.resolve(itemId)).isNull()
    }
}
