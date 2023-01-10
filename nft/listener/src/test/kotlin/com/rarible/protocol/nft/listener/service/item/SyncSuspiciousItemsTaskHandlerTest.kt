package com.rarible.protocol.nft.listener.service.item

import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.data.randomItemExState
import com.rarible.protocol.nft.core.repository.data.createItem
import com.rarible.protocol.nft.core.repository.item.ItemExStateRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.ReduceEventListenerListener
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
    private val eventListener = mockk<ReduceEventListenerListener>()
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
        every { itemRepository.findById(state1.id) } returns Mono.just(item1)
        every { itemRepository.findById(state2.id) } returns Mono.just(item2)
        every { eventListener.onItemChanged(item1) } returns Mono.empty()
        every { eventListener.onItemChanged(item2) } returns Mono.empty()

        val savedItemId = task.runLongTask(from.stringValue, "").toList().last()
        assertThat(savedItemId).isEqualTo(state2.id.stringValue)

        verify(exactly = 1) {
            itemExStateRepository.getAll(from)
            itemRepository.findById(state1.id)
            itemRepository.findById(state2.id)
            eventListener.onItemChanged(item1)
            eventListener.onItemChanged(item2)
        }
    }
}