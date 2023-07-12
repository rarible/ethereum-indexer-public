package com.rarible.protocol.nft.core.service.item.reduce.status

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.service.item.reduce.forward.ForwardChainItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.reversed.ReversedChainItemReducer
import com.rarible.protocol.nft.core.service.item.reduce.reversed.RevertItemCompactEventsReducer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class EventStatusItemReducerTest {
    private val forwardChainItemReducer = mockk<ForwardChainItemReducer>()
    private val reversedChainItemReducer = mockk<ReversedChainItemReducer>()

    private val eventStatusItemReducer = EventStatusItemReducer(
        forwardChainItemReducer = forwardChainItemReducer,
        reversedChainItemReducer = reversedChainItemReducer,
        revertItemCompactEventsReducer = RevertItemCompactEventsReducer(),
    )

    @Test
    fun `should handle confirm event`() = runBlocking<Unit> {
        val event = createRandomMintItemEvent()
            .let { it.copy(log = it.log.copy(status = EthereumBlockStatus.CONFIRMED)) }
        val item = createRandomItem()

        coEvery { forwardChainItemReducer.reduce(item, event) } returns item
        val reducedItem = eventStatusItemReducer.reduce(item, event)
        assertThat(reducedItem).isEqualTo(item)

        coVerify { forwardChainItemReducer.reduce(item, event) }
        coVerify(exactly = 0) { reversedChainItemReducer.reduce(any(), any()) }
    }

    @Test
    fun `should handle revert event`() = runBlocking<Unit> {
        val event = createRandomMintItemEvent()
            .let { it.copy(log = it.log.copy(status = EthereumBlockStatus.REVERTED)) }
        val item = createRandomItem()

        coEvery { reversedChainItemReducer.reduce(item, event) } returns item
        val reducedItem = eventStatusItemReducer.reduce(item, event)
        assertThat(reducedItem).isEqualTo(item)

        coVerify { reversedChainItemReducer.reduce(item, event) }
        coVerify(exactly = 0) { forwardChainItemReducer.reduce(any(), any()) }
    }

    @Test
    fun `should handle pending event`() = runBlocking<Unit> {
        val event = createRandomMintItemEvent().let { it.copy(log = it.log.copy(status = EthereumBlockStatus.PENDING)) }
        val item = createRandomItem()

        val reducedItem = eventStatusItemReducer.reduce(item, event)
        assertThat(reducedItem).isEqualTo(item)

        coVerify(exactly = 0) { forwardChainItemReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { reversedChainItemReducer.reduce(any(), any()) }
    }

    @Test
    fun `should handle inactive event`() = runBlocking<Unit> {
        val event = createRandomMintItemEvent()
            .let { it.copy(log = it.log.copy(status = EthereumBlockStatus.INACTIVE)) }
        val item = createRandomItem()

        val reducedItem = eventStatusItemReducer.reduce(item, event)
        assertThat(reducedItem).isEqualTo(item)

        coVerify(exactly = 0) { forwardChainItemReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { reversedChainItemReducer.reduce(any(), any()) }
    }

    @Test
    fun `should handle drop event`() = runBlocking<Unit> {
        val event = createRandomMintItemEvent()
            .let { it.copy(log = it.log.copy(status = EthereumBlockStatus.DROPPED)) }
        val item = createRandomItem()

        val reducedItem = eventStatusItemReducer.reduce(item, event)
        assertThat(reducedItem).isEqualTo(item)

        coVerify(exactly = 0) { forwardChainItemReducer.reduce(any(), any()) }
        coVerify(exactly = 0) { reversedChainItemReducer.reduce(any(), any()) }
    }
}
