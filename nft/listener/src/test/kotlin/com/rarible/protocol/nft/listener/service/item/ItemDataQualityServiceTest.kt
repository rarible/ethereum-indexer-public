package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemContinuation
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.data.createRandomItem
import com.rarible.protocol.nft.listener.data.createRandomOwnership
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

@IntegrationTest
@FlowPreview
internal class ItemDataQualityServiceTest : AbstractIntegrationTest() {
    @BeforeEach
    fun setupDbIndexes() = runBlocking<Unit> {
        itemRepository.createIndexes()
    }

    @Test
    fun `should alert invalid items`() = runBlocking<Unit> {
        val now = nowMillis()
        val counter = mockk<RegisteredCounter> {
            every { increment() } returns Unit
        }
        val itemDataQualityService = ItemDataQualityService(
            itemRepository = itemRepository,
            ownershipRepository = ownershipRepository,
            itemDataQualityErrorRegisteredCounter = counter,
            nftListenerProperties = NftListenerProperties().copy(elementsFetchJobSize = 2)
        )
        val validItem1 = createRandomItem().copy(supply = EthUInt256.of(4), date = now - Duration.ofMinutes(5))
        val invalidItem1 = createRandomItem().copy(supply = EthUInt256.of(4), date = now - Duration.ofMinutes(4))
        listOf(validItem1, invalidItem1).forEach { itemRepository.save(it).awaitFirst() }
        listOf(
            createValidOwnerships(validItem1),
            createInvalidValidOwnerships(invalidItem1),
        ).flatten().forEach { ownershipRepository.save(it).awaitFirst() }
        var continuations = itemDataQualityService.checkItems(from = null).toList()
        assertThat(continuations).hasSize(2)
        assertThat(continuations[0].let { ItemContinuation.parse(it)?.afterId }).isEqualTo(validItem1.id)
        assertThat(continuations[1].let { ItemContinuation.parse(it)?.afterId }).isEqualTo(invalidItem1.id)

        verify(exactly = 1) { counter.increment() }

        val validItem2 = createRandomItem().copy(supply = EthUInt256.of(4), date = now - Duration.ofMinutes(3))
        val invalidItem2 = createRandomItem().copy(supply = EthUInt256.of(4), date = now - Duration.ofMinutes(2))
        listOf(validItem2, invalidItem2).forEach { itemRepository.save(it).awaitFirst() }
        listOf(
            createValidOwnerships(validItem2),
            createInvalidValidOwnerships(invalidItem2)
        ).flatten().forEach { ownershipRepository.save(it).awaitFirst() }

        continuations = itemDataQualityService.checkItems(from = continuations.last()).toList()
        assertThat(continuations).hasSize(2)
        assertThat(continuations[0].let { ItemContinuation.parse(it)?.afterId }).isEqualTo(validItem2.id)
        assertThat(continuations[1].let { ItemContinuation.parse(it)?.afterId }).isEqualTo(invalidItem2.id)
        verify(exactly = 2) { counter.increment() }
    }

    private fun createValidOwnerships(item: Item): List<Ownership> {
        return (1..4).map {
            createRandomOwnership().copy(
                token = item.token,
                tokenId = item.tokenId,
                value = item.supply / EthUInt256.of(4)
            )
        }
    }

    private fun createInvalidValidOwnerships(item: Item): List<Ownership> {
        return (1..4).map {
            createRandomOwnership().copy(
                token = item.token,
                tokenId = item.tokenId,
                value = item.supply
            )
        }
    }
}