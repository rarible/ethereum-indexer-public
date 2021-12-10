package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.protocol.nft.core.data.*
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.EntityEventRevertService
import com.rarible.protocol.nft.core.service.item.reduce.forward.ForwardValueItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.lazy.LazyItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.status.EventStatusItemReducer
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class ItemReducerTest {
    private val lazyItemReducer = mockk<LazyItemReducer>()
    private val eventStatusItemReducer = mockk<EventStatusItemReducer>()
    private val itemReducer = ItemReducer(eventStatusItemReducer, lazyItemReducer)

    companion object {
        @JvmStatic
        fun blockchainEvents() = Stream.of(
            createRandomBurnItemEvent(),
            createRandomMintItemEvent()
        )

        @JvmStatic
        fun lazyEvents() = Stream.of(
            createRandomLazyMintItemEvent(),
            createRandomLazyBurnItemEvent()
        )
    }

    @ParameterizedTest
    @MethodSource("blockchainEvents")
    fun `should reduce blockchain events`(event: ItemEvent) = runBlocking<Unit> {
        val item = createRandomItem()

        coEvery { eventStatusItemReducer.reduce(item, event) } returns item
        itemReducer.reduce(item, event)

        coVerify(exactly = 1) { eventStatusItemReducer.reduce(item, event) }
        coVerify(exactly = 0) { lazyItemReducer.reduce(any(), any()) }
    }

    @ParameterizedTest
    @MethodSource("lazyEvents")
    fun `should reduce lazy events`(event: ItemEvent) = runBlocking<Unit> {
        val item = createRandomItem()

        coEvery { lazyItemReducer.reduce(item, event) } returns item
        itemReducer.reduce(item, event)

        coVerify(exactly = 1) { lazyItemReducer.reduce(item, event) }
        coVerify(exactly = 0) { eventStatusItemReducer.reduce(any(), any()) }
    }
}
