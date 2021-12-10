package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.core.common.nowMillis
import com.rarible.core.entity.reducer.exception.ReduceException
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.*
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.item.reduce.lazy.LazyItemReducer
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.util.stream.Stream

internal class LazyItemReducerTest {
    private val lazyItemReducer = LazyItemReducer()

    @Test
    fun `should reduce lazy mint event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(lazySupply = EthUInt256.ZERO, deleted = true, lastLazyEventTimestamp = Instant.EPOCH.epochSecond)
        val event = createRandomLazyMintItemEvent().copy(supply = EthUInt256.ONE, timestamp = nowMillis().epochSecond)

        val reducedItem = lazyItemReducer.reduce(item, event)

        Assertions.assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ONE)
        Assertions.assertThat(reducedItem.deleted).isEqualTo(false)
    }

    @Test
    fun `should reduce lazy burn event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(lazySupply = EthUInt256.ONE, deleted = false, lastLazyEventTimestamp = Instant.EPOCH.epochSecond)
        val event = createRandomLazyBurnItemEvent().copy(supply = EthUInt256.ONE)

        val reducedItem = lazyItemReducer.reduce(item, event)

        Assertions.assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        Assertions.assertThat(reducedItem.deleted).isEqualTo(true)
    }

    @Test
    fun `should not reduce old lazy event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(lastLazyEventTimestamp = nowMillis().epochSecond)
        val event = createRandomLazyBurnItemEvent().copy(timestamp = requireNotNull(item.lastLazyEventTimestamp) - 10)

        val reducedItem = lazyItemReducer.reduce(item, event)

        Assertions.assertThat(item).isEqualTo(reducedItem)
    }

    companion object {
        @JvmStatic
        fun invalidReduceEvents() = Stream.of(createRandomMintItemEvent(), createRandomBurnItemEvent())
    }

    @ParameterizedTest
    @MethodSource("invalidReduceEvents")
    fun `should throw exception on invalid event`(event: ItemEvent) = runBlocking<Unit> {
        org.junit.jupiter.api.assertThrows<ReduceException> {
            runBlocking {
                lazyItemReducer.reduce(createRandomItem(), event)
            }
        }
    }
}
