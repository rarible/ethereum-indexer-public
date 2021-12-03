package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.common.justOrEmpty
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.PendingLogItemProperties
import com.rarible.protocol.nft.core.repository.PendingLogItemPropertiesRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.meta.descriptors.PendingLogItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RariblePropertiesResolver
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import scalether.domain.Address

@ItemMetaTest
class PendingLogItemPropertiesResolverTest : BasePropertiesResolverTest() {

    private val pendingLogItemPropertiesRepository = mockk<PendingLogItemPropertiesRepository>()
    private val rariblePropertiesResolver = mockk<RariblePropertiesResolver>()
    private val itemRepository = mockk<ItemRepository>()
    private val pendingLogItemPropertiesResolver = PendingLogItemPropertiesResolver(
        pendingLogItemPropertiesRepository,
        itemRepository,
        rariblePropertiesResolver
    )

    @Test
    fun `pending log item properties resolver`() = runBlocking<Unit> {
        val token = randomAddress()
        val tokenId = EthUInt256(randomBigInt())
        val itemId = ItemId(token, tokenId)

        val tokenUri = "lazyTokenUri"
        val itemProperties = randomItemProperties()
        coEvery { rariblePropertiesResolver.resolveByTokenUri(itemId, tokenUri) } returns itemProperties

        justRun { pendingLogItemPropertiesRepository.save<PendingLogItemProperties>(any()) }
        pendingLogItemPropertiesResolver.savePendingLogItemPropertiesByUri(itemId, tokenUri)

        verify(exactly = 1) {
            pendingLogItemPropertiesRepository.save(withArg<PendingLogItemProperties> {
                assertThat(it.id).isEqualTo(itemId.decimalStringValue)
                assertThat(it.value).isEqualTo(itemProperties)
            })
        }

        val item = Item(
            token = itemId.token,
            tokenId = itemId.tokenId,
            date = nowMillis(),
            supply = EthUInt256.ZERO,
            royalties = emptyList()
        )
        every { itemRepository.findById(itemId) } returns
                item
                    .copy(
                        pending = listOf(
                            // Pending minting log event.
                            ItemTransfer(randomAddress(), itemId.token, itemId.tokenId, nowMillis(), Address.ZERO())
                        )
                    )
                    .justOrEmpty()

        every { pendingLogItemPropertiesRepository.findById(itemId.decimalStringValue) } returns
                PendingLogItemProperties(
                    id = itemId.decimalStringValue,
                    value = itemProperties,
                    createDate = nowMillis()
                ).justOrEmpty()

        val resolved = pendingLogItemPropertiesResolver.resolve(itemId)
        verify(exactly = 1) { pendingLogItemPropertiesRepository.findById(itemId.decimalStringValue) }
        assertThat(resolved).isEqualTo(itemProperties)

        // Then the item becomes confirmed and the resolver must return null and delete the pending log item properties.
        clearMocks(itemRepository)
        every { pendingLogItemPropertiesRepository.deleteById(itemId.decimalStringValue) } returns Mono.empty()

        every { itemRepository.findById(itemId) } returns
                item
                    .copy(supply = EthUInt256.ONE)
                    .justOrEmpty()

        assertThat(pendingLogItemPropertiesResolver.resolve(itemId)).isNull()
        verify(exactly = 1) { pendingLogItemPropertiesRepository.deleteById(itemId.decimalStringValue) }
    }
}
