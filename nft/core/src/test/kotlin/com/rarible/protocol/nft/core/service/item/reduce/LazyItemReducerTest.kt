package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomBurnItemEvent
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomLazyBurnItemEvent
import com.rarible.protocol.nft.core.data.createRandomLazyMintItemEvent
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.data.withNewValues
import com.rarible.protocol.nft.core.service.item.reduce.lazy.LazyItemReducer
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.stream.Stream

internal class LazyItemReducerTest {
    private val lazyItemReducer = LazyItemReducer()

    @Test
    fun `should reduce lazy mint event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(
            lazySupply = EthUInt256.ZERO,
            deleted = true,
            lastLazyEventTimestamp = Instant.EPOCH.epochSecond
        )
        val event = createRandomLazyMintItemEvent().copy(supply = EthUInt256.ONE)

        val reducedItem = lazyItemReducer.reduce(item, event)

        Assertions.assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ONE)
    }

    @Test
    fun `should reduce lazy burn event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(
            lazySupply = EthUInt256.ONE,
            supply = EthUInt256.ZERO,
            deleted = false,
            lastLazyEventTimestamp = Instant.EPOCH.epochSecond
        )
        val event = createRandomLazyBurnItemEvent().copy(supply = EthUInt256.ONE)

        val reducedItem = lazyItemReducer.reduce(item, event)

        Assertions.assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `should not reduce old lazy event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(lastLazyEventTimestamp = nowMillis().epochSecond)
        val baseEvent = createRandomLazyBurnItemEvent()
        val event = baseEvent.withNewValues(
            createdAt = Instant.ofEpochSecond(item.lastLazyEventTimestamp!!).minusSeconds(10)
        )

        val reducedItem = lazyItemReducer.reduce(item, event)

        Assertions.assertThat(item).isEqualTo(reducedItem)
    }

    companion object {
        @JvmStatic
        fun invalidReduceEvents() = Stream.of(createRandomMintItemEvent(), createRandomBurnItemEvent())
    }
}
