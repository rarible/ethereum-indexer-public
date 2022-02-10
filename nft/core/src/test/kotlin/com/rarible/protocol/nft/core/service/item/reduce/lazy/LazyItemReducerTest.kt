package com.rarible.protocol.nft.core.service.item.reduce.lazy

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomLazyBurnItemEvent
import com.rarible.protocol.nft.core.data.createRandomLazyMintItemEvent
import com.rarible.protocol.nft.core.model.Part
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

internal class LazyItemReducerTest {
    private val lazyItemReducer = LazyItemReducer()

    @Test
    fun `should handle lazy mint event`() = runBlocking<Unit> {
        val event = createRandomLazyMintItemEvent().copy(
            supply = EthUInt256.TEN,
            creators = listOf(Part(randomAddress(), 1), Part(randomAddress(), 2))
        ).let {
            it.copy(log = it.log.copy(createdAt = Instant.EPOCH))
        }
        val item = createRandomItem().copy(lastLazyEventTimestamp = null)

        val reducedItem = lazyItemReducer.reduce(item, event)
        assertThat(reducedItem.lazySupply).isEqualTo(event.supply)
        assertThat(reducedItem.supply).isEqualTo(event.supply)
        assertThat(reducedItem.lastLazyEventTimestamp).isEqualTo(event.timestamp)
        assertThat(reducedItem.creators).isEqualTo(event.creators)
        assertThat(reducedItem.creatorsFinal).isTrue()
    }

    @Test
    fun `should handle lazy burn event`() = runBlocking<Unit> {
        val event = createRandomLazyBurnItemEvent().copy(
            supply = EthUInt256.TEN
        ).let {
            it.copy(log = it.log.copy(createdAt = Instant.EPOCH))
        }
        val item = createRandomItem().copy(lastLazyEventTimestamp = null)

        val reducedItem = lazyItemReducer.reduce(item, event)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.lastLazyEventTimestamp).isEqualTo(event.timestamp)
    }
}
