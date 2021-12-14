package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ForwardLazyValueItemReducerTest {
    private val forwardLazyValueItemReducer = ForwardLazyValueItemReducer()

    @Test
    fun `should calculate value and lazyValue on mint event`() = runBlocking<Unit> {
        val event = createRandomMintItemEvent().copy(supply = EthUInt256.of(2))

        val item = createRandomItem().copy(
            supply = EthUInt256.TEN,
            lazySupply = EthUInt256.TEN,
            lastLazyEventTimestamp = 12
        )
        val reducedItem = forwardLazyValueItemReducer.reduce(item, event)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.of(8))
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.of(8))
    }

    @Test
    fun `should skip calculation if it is not lazy item`() = runBlocking<Unit> {
        val event = createRandomMintItemEvent().copy(supply = EthUInt256.of(2))

        val item = createRandomItem().copy(
            supply = EthUInt256.TEN,
            lazySupply = EthUInt256.ZERO,
            lastLazyEventTimestamp = null
        )
        val reducedItem = forwardLazyValueItemReducer.reduce(item, event)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.TEN)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
    }
}
