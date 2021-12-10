package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.protocol.nft.core.data.*
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.EntityEventRevertService
import com.rarible.protocol.nft.core.service.item.reduce.forward.ForwardValueItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.lazy.LazyItemReducer
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class ItemReducerTest {
    private val lazyItemReducer = mockk<LazyItemReducer>()
    private val blockchainItemReducer = mockk<ForwardValueItemReducer>()
    private val entityEventRevertService = mockk<EntityEventRevertService<ItemEvent>>()
    private val itemReducer = ItemReducer(lazyItemReducer, blockchainItemReducer, entityEventRevertService)

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

        coEvery { blockchainItemReducer.reduce(item, event) } returns item
        coEvery { entityEventRevertService.canBeReverted(event, event) } returns false
        itemReducer.reduce(item, event)

        coVerify(exactly = 1) { blockchainItemReducer.reduce(item, event) }
        coVerify(exactly = 1) { entityEventRevertService.canBeReverted(event, event) }
        coVerify(exactly = 0) { lazyItemReducer.reduce(any(), any()) }
    }

    @ParameterizedTest
    @MethodSource("lazyEvents")
    fun `should reduce lazy events`(event: ItemEvent) = runBlocking<Unit> {
        val item = createRandomItem()

        coEvery { lazyItemReducer.reduce(item, event) } returns item
        itemReducer.reduce(item, event)

        coVerify(exactly = 1) { lazyItemReducer.reduce(item, event) }
        coVerify(exactly = 0) { blockchainItemReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { entityEventRevertService.canBeReverted(any(), any()) }
    }
}
