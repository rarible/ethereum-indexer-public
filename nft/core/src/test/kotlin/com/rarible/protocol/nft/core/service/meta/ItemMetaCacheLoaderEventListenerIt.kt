package com.rarible.protocol.nft.core.service.meta

import com.rarible.core.common.convert
import com.rarible.core.loader.internal.RetryTasksService
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.randomItemMeta
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.ExtendedItem
import io.mockk.coEvery
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Test that the [com.rarible.protocol.nft.core.service.item.meta.ItemMetaCacheLoaderEventListener]
 * catches up the meta loading event and sends an Item update event.
 */
@IntegrationTest
class ItemMetaCacheLoaderEventListenerIt : AbstractIntegrationTest() {
    @Test
    fun `send notification when meta is loaded or updated`() = runBlocking<Unit> {
        val item = createRandomItem()
        val itemMeta = randomItemMeta()
        val itemId = item.id
        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } returns itemMeta
        itemRepository.save(item).awaitFirst()

        itemMetaService.scheduleMetaUpdate(itemId)
        val itemDto = conversionService.convert<NftItemDto>(ExtendedItem(item, itemMeta))
        Wait.waitAssert {
            assertThat(itemEvents).anySatisfy { event ->
                assertThat(event).isInstanceOfSatisfying(NftItemUpdateEventDto::class.java) {
                    assertThat(it.item).isEqualTo(itemDto)
                }
            }
        }

        // Update the meta.
        val itemMeta2 = randomItemMeta()
        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } returns itemMeta2
        itemMetaService.scheduleMetaUpdate(itemId)
        val itemDto2 = conversionService.convert<NftItemDto>(ExtendedItem(item, itemMeta2))
        Wait.waitAssert {
            assertThat(itemEvents).anySatisfy { event ->
                assertThat(event).isInstanceOfSatisfying(NftItemUpdateEventDto::class.java) {
                    assertThat(it.item).isEqualTo(itemDto2)
                }
            }
        }
    }

    @Autowired
    private lateinit var retryTasksService: RetryTasksService

    @Test
    fun `send notification with empty meta on failed - then with loaded meta after retry`() = runBlocking<Unit> {
        val item = createRandomItem()
        val itemId = item.id
        val itemMeta = randomItemMeta()
        val error = RuntimeException("loading error")
        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } throws(error)
        itemRepository.save(item).awaitFirst()

        itemMetaService.scheduleMetaUpdate(itemId)
        delay(1000)
        assertThat(itemMetaService.getAvailable(itemId)).isNull()
        assertThat(itemMetaService.isMetaInitiallyLoadedOrFailed(itemId)).isTrue
        val emptyMetaItemDto = conversionService.convert<NftItemDto>(ExtendedItem(item, null))
        Wait.waitAssert {
            assertThat(itemEvents).anySatisfy { event ->
                assertThat(event).isInstanceOfSatisfying(NftItemUpdateEventDto::class.java) {
                    assertThat(it.item).isEqualTo(emptyMetaItemDto)
                }
            }
        }
        itemEvents.clear()

        // Initiate retrying of tasks.
        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } returns(itemMeta)
        retryTasksService.scheduleTasksToRetry()

        val itemDto = conversionService.convert<NftItemDto>(ExtendedItem(item, itemMeta))
        Wait.waitAssert {
            assertThat(itemMetaService.getAvailableMetaOrScheduleLoading(itemId)).isEqualTo(itemMeta)

            assertThat(itemEvents).anySatisfy { event ->
                assertThat(event).isInstanceOfSatisfying(NftItemUpdateEventDto::class.java) {
                    assertThat(it.item).isEqualTo(itemDto)
                }
            }
        }
    }

    @Test
    fun `do not send notification when meta update failed`() = runBlocking<Unit> {
        val item = createRandomItem()
        val itemMeta = randomItemMeta()
        val itemId = item.id
        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } returns itemMeta
        itemRepository.save(item).awaitFirst()

        itemMetaService.scheduleMetaUpdate(itemId)
        Wait.waitAssert {
            assertThat(itemEvents).anySatisfy { assertThat(it.itemId).isEqualTo(itemId.decimalStringValue) }
        }
        itemEvents.clear()

        val error = RuntimeException("update error")
        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } throws error
        itemMetaService.scheduleMetaUpdate(itemId)

        delay(3000)
        assertThat(itemEvents).noneSatisfy { event ->
            assertThat(event.itemId).isEqualTo(itemId.decimalStringValue)
        }
    }

}
