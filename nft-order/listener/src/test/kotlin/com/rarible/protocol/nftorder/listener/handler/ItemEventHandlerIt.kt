package com.rarible.protocol.nftorder.listener.handler

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.service.ItemService
import com.rarible.protocol.nftorder.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nftorder.listener.test.IntegrationTest
import com.rarible.protocol.nftorder.listener.test.data.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class ItemEventHandlerIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var itemEventHandler: ItemEventHandler

    @Autowired
    private lateinit var itemService: ItemService

    @Test
    fun `update event - item fetched and stored`() = runWithKafka {
        val itemId = randomItemId()
        val bestSell = randomLegacyOrderDto(itemId)
        val bestBid = randomLegacyOrderDto(itemId)

        // Enrichment requests
        lockControllerApiMock.mockIsUnlockable(itemId, true)
        orderControllerApiMock.mockGetSellOrdersByItem(itemId, bestSell)
        orderControllerApiMock.mockGetBidOrdersByItem(itemId, bestBid)

        val nftItemDto = randomNftItemDto(itemId)
        itemEventHandler.handle(createItemUpdateEvent(nftItemDto))

        val created = itemService.get(itemId)!!

        assertItemAndDtoEquals(created, nftItemDto)
        assertThat(created.unlockable).isTrue()
        assertThat(created.bestSellOrder).isEqualTo(bestSell)
        assertThat(created.bestBidOrder).isEqualTo(bestBid)
        Wait.waitAssert {
            assertThat(itemEvents).hasSize(1)
            assertUpdateItemEvent(itemId, itemEvents!![0])
        }
    }

    @Test
    fun `update event - existing item updated`() = runWithKafka {
        val item = itemService.save(randomItem())
        val itemId = item.id
        val bestSell = randomLegacyOrderDto(itemId)
        val bestBid = randomLegacyOrderDto(itemId)

        // Despite we already have stored enrichment data, we refreshing it on update
        lockControllerApiMock.mockIsUnlockable(itemId, false)
        orderControllerApiMock.mockGetSellOrdersByItem(itemId, bestSell)
        orderControllerApiMock.mockGetBidOrdersByItem(itemId, bestBid)

        val nftItemDto = randomNftItemDto(itemId)
        itemEventHandler.handle(createItemUpdateEvent(nftItemDto))

        val created = itemService.get(itemId)!!

        // Entity should be completely replaced by update data
        assertItemAndDtoEquals(created, nftItemDto)
        assertThat(created.unlockable).isFalse()
        assertThat(created.bestSellOrder).isEqualTo(bestSell)
        assertThat(created.bestBidOrder).isEqualTo(bestBid)
        Wait.waitAssert {
            assertThat(itemEvents).hasSize(1)
            assertUpdateItemEvent(itemId, itemEvents!![0])
        }
    }

    @Test
    fun `update event - existing item deleted, no enrich data`() = runWithKafka {
        val item = itemService.save(randomItem())
        assertThat(itemService.get(item.id)).isNotNull()

        // No enrichment data fetched
        lockControllerApiMock.mockIsUnlockable(item.id, false)
        orderControllerApiMock.mockGetSellOrdersByItem(item.id)
        orderControllerApiMock.mockGetBidOrdersByItem(item.id)
        orderControllerApiMock.mockGetSellOrdersByItem(item.id)

        val nftItemDto = randomNftItemDto(item.id)
        itemEventHandler.handle(createItemUpdateEvent(nftItemDto))

        // Entity removed due to absence of enrichment data
        assertThat(itemService.get(item.id)).isNull()
        Wait.waitAssert {
            assertThat(itemEvents).hasSize(1)
            assertUpdateItemEvent(item.id, itemEvents!![0])
        }
    }

    @Test
    fun `update event - update skipped, no enrich data`() = runWithKafka<Unit> {
        val itemId = randomItemId()

        // No enrichment data fetched
        lockControllerApiMock.mockIsUnlockable(itemId, false)
        orderControllerApiMock.mockGetSellOrdersByItem(itemId)
        orderControllerApiMock.mockGetBidOrdersByItem(itemId)
        orderControllerApiMock.mockGetSellOrdersByItem(itemId)

        val nftItemDto = randomNftItemDto(itemId)
        itemEventHandler.handle(createItemUpdateEvent(nftItemDto))

        // Entity not created due to absence of enrichment data
        assertThat(itemService.get(itemId)).isNull()
        Wait.waitAssert {
            assertThat(itemEvents).hasSize(1)
            assertUpdateItemEvent(itemId, itemEvents!![0])
        }
    }

    @Test
    fun `delete event - existing item deleted`() = runWithKafka {
        val item = itemService.save(randomItem())
        assertThat(itemService.get(item.id)).isNotNull()

        itemEventHandler.handle(createItemDeleteEvent(item.id))

        assertThat(itemService.get(item.id)).isNull()
        Wait.waitAssert {
            assertThat(itemEvents).hasSize(1)
            assertDeleteItemEvent(item.id, itemEvents!![0])
        }
    }

    @Test
    fun `delete event - item doesn't exist`() = runWithKafka {
        val itemId = randomItemId()

        itemEventHandler.handle(createItemDeleteEvent(itemId))

        assertThat(itemService.get(itemId)).isNull()
        Wait.waitAssert {
            assertThat(itemEvents).hasSize(1)
            assertDeleteItemEvent(itemId, itemEvents!![0])
        }
    }

    private fun assertDeleteItemEvent(itemId: ItemId, message: KafkaMessage<NftOrderItemEventDto>) {
        val event = message.value
        assertThat(event is NftOrderItemDeleteEventDto)
        assertThat(event.itemId).isEqualTo(itemId.decimalStringValue)
    }

    private fun assertUpdateItemEvent(itemId: ItemId, message: KafkaMessage<NftOrderItemEventDto>) {
        val event = message.value
        assertThat(event is NftOrderItemUpdateEventDto)
        assertThat(event.itemId).isEqualTo(itemId.decimalStringValue)
    }

    private fun createItemUpdateEvent(nftItem: NftItemDto): NftItemUpdateEventDto {
        return NftItemUpdateEventDto(
            randomString(),
            nftItem.id,
            nftItem
        )
    }

    private fun createItemDeleteEvent(itemId: ItemId): NftItemDeleteEventDto {
        return NftItemDeleteEventDto(
            randomString(),
            itemId.stringValue,
            NftDeletedItemDto(
                itemId.stringValue,
                itemId.token,
                itemId.tokenId.value
            )
        )
    }
}