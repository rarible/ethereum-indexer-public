package com.rarible.protocol.nft.core.service.item.reduce.status

import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.model.BlockchainEntityEvent
import com.rarible.protocol.nft.core.service.item.reduce.forward.ForwardChainItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.inactive.InactiveChainItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.pending.PendingChainItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.reversed.ReversedChainItemReducer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class EventStatusItemReducerTest {
    private val forwardChainItemReducer = mockk<ForwardChainItemReducer>()
    private val reversedChainItemReducer = mockk<ReversedChainItemReducer>()
    private val pendingChainItemReducer = mockk<PendingChainItemReducer>()
    private val inactiveChainItemReducer = mockk<InactiveChainItemReducer>()

    private val eventStatusItemReducer = EventStatusItemReducer(
        forwardChainItemReducer = forwardChainItemReducer,
        reversedChainItemReducer = reversedChainItemReducer,
        pendingChainItemReducer = pendingChainItemReducer,
        inactiveChainItemReducer = inactiveChainItemReducer
    )

    @Test
    fun `should handle confirm event`() = runBlocking<Unit> {
        val event = createRandomMintItemEvent().copy(status = BlockchainEntityEvent.Status.CONFIRMED)
        val item = createRandomItem()

        coEvery { forwardChainItemReducer.reduce(item, event) } returns item
        val reducedItem = eventStatusItemReducer.reduce(item, event)
        assertThat(reducedItem).isEqualTo(item)

        coVerify { forwardChainItemReducer.reduce(item, event) }
        coVerify(exactly = 0) { reversedChainItemReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { pendingChainItemReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { inactiveChainItemReducer.reduce(any(), any()) }
    }

    @Test
    fun `should handle revert event`() = runBlocking<Unit> {
        val event = createRandomMintItemEvent().copy(status = BlockchainEntityEvent.Status.REVERTED)
        val item = createRandomItem()

        coEvery { reversedChainItemReducer.reduce(item, event) } returns item
        val reducedItem = eventStatusItemReducer.reduce(item, event)
        assertThat(reducedItem).isEqualTo(item)

        coVerify { reversedChainItemReducer.reduce(item, event) }
        coVerify(exactly = 0) { forwardChainItemReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { pendingChainItemReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { inactiveChainItemReducer.reduce(any(), any()) }
    }

    @Test
    fun `should handle pending event`() = runBlocking<Unit> {
        val event = createRandomMintItemEvent().copy(status = BlockchainEntityEvent.Status.PENDING)
        val item = createRandomItem()

        coEvery { pendingChainItemReducer.reduce(item, event) } returns item
        val reducedItem = eventStatusItemReducer.reduce(item, event)
        assertThat(reducedItem).isEqualTo(item)

        coVerify { pendingChainItemReducer.reduce(item, event) }
        coVerify(exactly = 0) { forwardChainItemReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { reversedChainItemReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { inactiveChainItemReducer.reduce(any(), any()) }
    }

    @Test
    fun `should handle inactive event`() = runBlocking<Unit> {
        val event = createRandomMintItemEvent().copy(status = BlockchainEntityEvent.Status.INACTIVE)
        val item = createRandomItem()

        coEvery { inactiveChainItemReducer.reduce(item, event) } returns item
        val reducedItem = eventStatusItemReducer.reduce(item, event)
        assertThat(reducedItem).isEqualTo(item)

        coVerify { inactiveChainItemReducer.reduce(item, event) }
        coVerify(exactly = 0) { forwardChainItemReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { reversedChainItemReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { pendingChainItemReducer.reduce(any(), any()) }
    }

    @Test
    fun `should handle drop event`() = runBlocking<Unit> {
        val event = createRandomMintItemEvent().copy(status = BlockchainEntityEvent.Status.DROPPED)
        val item = createRandomItem()

        coEvery { inactiveChainItemReducer.reduce(item, event) } returns item
        val reducedItem = eventStatusItemReducer.reduce(item, event)
        assertThat(reducedItem).isEqualTo(item)

        coVerify { inactiveChainItemReducer.reduce(item, event) }
        coVerify(exactly = 0) { forwardChainItemReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { reversedChainItemReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { pendingChainItemReducer.reduce(any(), any()) }
    }
}
