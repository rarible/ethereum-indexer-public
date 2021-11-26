package com.rarible.protocol.nftorder.api.controller

import com.rarible.core.kafka.KafkaMessage
import com.rarible.protocol.dto.NftOrderItemDto
import com.rarible.protocol.dto.NftOrderItemEventDto
import com.rarible.protocol.nftorder.api.test.AbstractFunctionalTest
import com.rarible.protocol.nftorder.api.test.FunctionalTest
import com.rarible.protocol.nftorder.listener.test.mock.data.*
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@FunctionalTest
class RefreshControllerFt : AbstractFunctionalTest() {

    @Test
    fun `refresh item only`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val nftItemDto = randomNftItemDto(itemId)
        val bestSell = randomOrderDto(itemId)
        val bestBid = randomOrderDto(itemId)

        val uri = "$baseUri/v0.1/refresh/item/${itemId.decimalStringValue}"

        nftItemControllerApiMock.mockGetNftItemById(itemId, nftItemDto)
        orderControllerApiMock.mockGetSellOrdersByItem(itemId, bestSell)
        orderControllerApiMock.mockGetBidOrdersByItem(itemId, bestBid)
        lockControllerApiMock.mockIsUnlockable(itemId, true)

        val result = testRestTemplate.postForEntity(uri, null, NftOrderItemDto::class.java).body!!

        assertThat(result.bestSellOrder).isEqualTo(bestSell)
        assertThat(result.bestBidOrder).isEqualTo(bestBid)
        assertThat(result.unlockable).isTrue()

        coVerify {
            testItemEventProducer.send(match<KafkaMessage<NftOrderItemEventDto>> { message ->
                message.value.itemId.equals(itemId.decimalStringValue)
            })
        }
    }

    @Test
    fun `refresh item with ownerships`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val ownershipId = randomOwnershipId(itemId)
        val nftItemDto = randomNftItemDto(itemId)
        val nftOwnershipDto = randomNftOwnershipDto(ownershipId)
        val bestSell = randomOrderDto(itemId)

        val uri = "$baseUri/v0.1/refresh/item/${itemId.decimalStringValue}?full=true"

        nftOwnershipControllerApiMock.mockGetNftOwnershipsByItem(itemId, nftOwnershipDto)
        nftItemControllerApiMock.mockGetNftItemById(itemId, nftItemDto)
        orderControllerApiMock.mockGetSellOrdersByItem(itemId)
        orderControllerApiMock.mockGetSellOrdersByOwnership(ownershipId, bestSell)
        orderControllerApiMock.mockGetBidOrdersByItem(itemId)
        lockControllerApiMock.mockIsUnlockable(itemId, false)

        val result = testRestTemplate.postForEntity(uri, null, NftOrderItemDto::class.java).body!!

        assertThat(result.bestSellOrder).isNull()
        assertThat(result.bestBidOrder).isNull()
        assertThat(result.unlockable).isFalse()
        assertThat(result.sellers).isEqualTo(1)
        assertThat(result.totalStock).isEqualTo(bestSell.makeStock)

        coVerify {
            testItemEventProducer.send(match<KafkaMessage<NftOrderItemEventDto>> { message ->
                message.value.itemId.equals(itemId.decimalStringValue)
            })
        }

    }

}
