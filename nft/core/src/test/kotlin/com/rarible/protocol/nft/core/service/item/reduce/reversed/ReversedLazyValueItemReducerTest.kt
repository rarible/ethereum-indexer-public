package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ReversedLazyValueItemReducerTest {
    private val reversedLazyValueItemReducer = ReversedLazyValueItemReducer()

    @Test
    fun `should revert supply and lazySupply on mint`() = runBlocking<Unit> {
        val item = createRandomItem().copy(
            supply = EthUInt256.of(9),
            lazySupply = EthUInt256.of(9),
            lastLazyEventTimestamp = 12
        )
        val event = createRandomMintItemEvent().copy(supply = EthUInt256.ONE)

        val reducedItem = reversedLazyValueItemReducer.reduce(item, event)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.TEN)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should do nothing if it is not lazy item`() = runBlocking<Unit> {
        val item = createRandomItem().copy(
            supply = EthUInt256.TEN,
            lazySupply = EthUInt256.TEN,
            lastLazyEventTimestamp = null
        )
        val event = createRandomMintItemEvent().copy(supply = EthUInt256.ONE)

        val reducedItem = reversedLazyValueItemReducer.reduce(item, event)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.TEN)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.TEN)
    }
}
