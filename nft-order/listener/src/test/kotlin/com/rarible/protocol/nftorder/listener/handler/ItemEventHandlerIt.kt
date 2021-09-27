package com.rarible.protocol.nftorder.listener.handler

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.core.converter.ShortOrderConverter
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.service.ItemService
import com.rarible.protocol.nftorder.listener.test.IntegrationTest
import com.rarible.protocol.nftorder.listener.test.data.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class ItemEventHandlerIt : AbstractEventHandlerIt() {

    @Autowired
    private lateinit var itemEventHandler: ItemEventHandler

    @Autowired
    private lateinit var itemService: ItemService

    @Test
    fun `update event - item doesn't exist`() = runWithKafka {
        val itemId = randomItemId()
        val nftItemDto = randomNftItemDto(itemId)

        itemEventHandler.handle(createItemUpdateEvent(nftItemDto))

        val created = itemService.get(itemId)

        // Item should not be updated since it wasn't in DB before update
        assertThat(created).isNull()
        // But there should be single Item event "as is"
        Wait.waitAssert {
            assertThat(itemEvents).hasSize(1)
            assertUpdateItemEvent(itemId, itemEvents!![0])
        }
    }

    @Test
    fun `update event - existing item updated`() = runWithKafka {
        val itemId = randomItemId()
        val bestSell = randomLegacyOrderDto(itemId)
        val bestBid = randomLegacyOrderDto(itemId)

        val item = randomItem(itemId).copy(
            bestSellOrder = ShortOrderConverter.convert(bestSell),
            bestBidOrder = ShortOrderConverter.convert(bestBid),
            unlockable = true
        )

        itemService.save(item)
        orderControllerApiMock.mockGetById(bestSell, bestBid)

        val nftItemDto = randomNftItemDto(itemId)
        itemEventHandler.handle(createItemUpdateEvent(nftItemDto))

        val created = itemService.get(itemId)!!

        // Entity should be completely replaced by update data, except enrich data - it should be the same
        assertItemAndDtoEquals(created, nftItemDto)
        assertThat(created.unlockable).isTrue()
        assertThat(created.bestSellOrder).isEqualTo(ShortOrderConverter.convert(bestSell))
        assertThat(created.bestBidOrder).isEqualTo(ShortOrderConverter.convert(bestBid))
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
