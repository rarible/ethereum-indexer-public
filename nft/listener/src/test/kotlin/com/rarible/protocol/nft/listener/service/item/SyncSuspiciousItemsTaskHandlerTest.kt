package com.rarible.protocol.nft.listener.service.item

import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.data.randomItemExState
import com.rarible.protocol.nft.core.repository.data.createItem
import com.rarible.protocol.nft.core.repository.item.ItemExStateRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceEventListener
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

@Suppress("ReactiveStreamsUnusedPublisher")
class SyncSuspiciousItemsTaskHandlerTest {
    private val itemExStateRepository = mockk<ItemExStateRepository>()
    private val itemRepository = mockk<ItemRepository>()
    private val eventListener = mockk<ItemReduceEventListener>()
    private val task = SyncSuspiciousItemsTaskHandler(itemExStateRepository, itemRepository, eventListener)

    @Test
    fun sync() = runBlocking<Unit> {
        val state1 = randomItemExState()
        val state2 = randomItemExState()
        val item1 = createItem(state1.id.token, state1.id.tokenId)
        val item2 = createItem(state2.id.token, state2.id.tokenId)

        val from = createRandomItemId()

        every { itemExStateRepository.getAll(from) } returns flow {
            emit(state1)
            emit(state2)
        }
        coEvery { itemRepository.searchByIds(setOf(state1.id, state2.id)) } returns listOf(item1, item2)
        every { eventListener.onItemChanged(item1, any()) } returns Mono.empty()
        every { eventListener.onItemChanged(item2, any()) } returns Mono.empty()

        val savedItemId = task.runLongTask(from.stringValue, "").toList().last()
        assertThat(savedItemId).isEqualTo(state2.id.stringValue)

        verify(exactly = 1) {
            itemExStateRepository.getAll(from)
            eventListener.onItemChanged(item1, any())
            eventListener.onItemChanged(item2, any())
        }
        coVerify(exactly = 1) {
            itemRepository.searchByIds(setOf(state1.id, state2.id))
        }
    }
}
