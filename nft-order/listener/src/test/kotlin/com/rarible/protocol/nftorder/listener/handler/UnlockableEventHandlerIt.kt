package com.rarible.protocol.nftorder.listener.handler

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.UnlockableEventDto
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.service.ItemService
import com.rarible.protocol.nftorder.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nftorder.listener.test.IntegrationTest
import com.rarible.protocol.nftorder.listener.test.data.randomItem
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class UnlockableEventHandlerIt : AbstractIntegrationTest() {

    @Autowired
    protected lateinit var itemService: ItemService

    @Autowired
    private lateinit var unlockableEventHandler: UnlockableEventHandler

    @Test
    fun `lock created event - item updated`() = runWithKafka {
        val item = itemService.save(randomItem())
        assertThat(item.unlockable).isFalse()

        unlockableEventHandler.handle(createUnlockableLockCreatedEvent(item.id))

        val updatedItem = itemService.get(item.id)!!

        assertThat(updatedItem.unlockable).isTrue()
        Wait.waitAssert {
            assertThat(itemEvents).hasSize(1)
        }
    }

    @Test
    fun `item unlocked event - event skipped`() = runBlocking<Unit> {
        val item = itemService.save(randomItem())
        assertThat(item.unlockable).isFalse()

        unlockableEventHandler.handle(createUnlockableUnlockedEvent(item.id))

        val updatedItem = itemService.get(item.id)!!
        assertThat(updatedItem.unlockable).isFalse()
    }

    private fun createUnlockableLockCreatedEvent(itemId: ItemId): UnlockableEventDto {
        return UnlockableEventDto(
            randomString(),
            itemId.stringValue,
            UnlockableEventDto.Type.LOCK_CREATED
        )
    }

    private fun createUnlockableUnlockedEvent(itemId: ItemId): UnlockableEventDto {
        return UnlockableEventDto(
            randomString(),
            itemId.stringValue,
            UnlockableEventDto.Type.LOCK_UNLOCKED
        )
    }
}