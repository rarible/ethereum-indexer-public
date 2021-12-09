package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.model.BlockchainEntityEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class CompositeItemReducerTest {
    private val itemReducer = mockk<ItemReducer>()
    private val reversedItemReducer = mockk<ReversedItemReducer>()

    private val compositeItemReducer = CompositeItemReducer(
        itemReducer = itemReducer,
        reversedItemReducer = reversedItemReducer
    )

    companion object {
        @JvmStatic
        fun normalStatus() = Stream.of(BlockchainEntityEvent.Status.CONFIRMED, BlockchainEntityEvent.Status.PENDING)

        @JvmStatic
        fun revertedStatus() = Stream.of(BlockchainEntityEvent.Status.REVERTED)
    }

    @ParameterizedTest
    @MethodSource("normalStatus")
    fun `should reduce normal blocks`(status: BlockchainEntityEvent.Status) = runBlocking<Unit> {
        val event = createRandomMintItemEvent().copy(status = status)
        val item = createRandomItem()

        coEvery { itemReducer.reduce(item, event) } returns item

        compositeItemReducer.reduce(item, event)

        coVerify(exactly = 1) { itemReducer.reduce(item, event) }
        coVerify(exactly = 0) { reversedItemReducer.reduce(any(), any()) }
    }

    @ParameterizedTest
    @MethodSource("revertedStatus")
    fun `should reduce reverted blocks`(status: BlockchainEntityEvent.Status) = runBlocking<Unit> {
        val event = createRandomMintItemEvent().copy(status = status)
        val item = createRandomItem()

        coEvery { reversedItemReducer.reduce(item, event) } returns item

        compositeItemReducer.reduce(item, event)

        coVerify(exactly = 0) { reversedItemReducer.reduce(item, event) }
        coVerify(exactly = 0) { itemReducer.reduce(any(), any()) }
    }
}
