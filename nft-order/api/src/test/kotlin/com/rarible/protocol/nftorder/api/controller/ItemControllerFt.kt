package com.rarible.protocol.nftorder.api.controller

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.client.exception.ProtocolApiResponseException
import com.rarible.protocol.dto.NftIndexerApiErrorDto
import com.rarible.protocol.dto.NftOrderApiErrorDto
import com.rarible.protocol.nftorder.api.client.NftOrderItemControllerApi
import com.rarible.protocol.nftorder.api.test.AbstractFunctionalTest
import com.rarible.protocol.nftorder.api.test.FunctionalTest
import com.rarible.protocol.nftorder.core.repository.ItemRepository
import com.rarible.protocol.nftorder.core.test.data.assertItemAndDtoEquals
import com.rarible.protocol.nftorder.core.test.data.assertItemDtoAndNftDtoEquals
import com.rarible.protocol.nftorder.listener.test.mock.data.*
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

@FunctionalTest
internal class ItemControllerFt : AbstractFunctionalTest() {

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var nftOrderItemControllerApi: NftOrderItemControllerApi

    @Test
    @Disabled
    fun `get item by id - not synced`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val nftItem = randomNftItemDto(itemId, randomPartDto())

        nftItemControllerApiMock.mockGetNftItemById(itemId, nftItem)

        val result = nftOrderItemControllerApi
            .getNftOrderItemById(itemId.decimalStringValue, null)
            .awaitFirst()!!

        assertThat(result.bestSellOrder).isNull()
        assertThat(result.bestBidOrder).isNull()
        assertThat(result.unlockable).isEqualTo(false)
        assertThat(result.meta).isNull()
        assertItemDtoAndNftDtoEquals(result, nftItem)
    }

    @Test
    fun `get item by id with meta - not synced`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val nftItem = randomNftItemDto(itemId, randomPartDto())
        val nftItemMeta = randomNftItemMetaDto()

        nftItemControllerApiMock.mockGetNftItemById(itemId, nftItem)
        lockControllerApiMock.mockIsUnlockable(itemId, false)
        orderControllerApiMock.mockGetSellOrdersByItem(itemId)
        orderControllerApiMock.mockGetBidOrdersByItem(itemId)
        nftItemControllerApiMock.mockGetNftItemMetaById(itemId, nftItemMeta)

        val result = nftOrderItemControllerApi
            .getNftOrderItemById(itemId.decimalStringValue, true)
            .awaitFirst()

        assertThat(result.meta).isEqualTo(nftItemMeta)
        assertItemDtoAndNftDtoEquals(result, nftItem)
    }

    @Test
    fun `get item by id - synced`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val bestSell = randomOrderDto(itemId)
        val bestBid = randomOrderDto(itemId)
        val item = randomItem(itemId).copy(
            bestSellOrder = bestSell,
            bestBidOrder = bestBid,
            totalStock = randomBigInt()
        )

        itemRepository.save(item)

        val result = nftOrderItemControllerApi
            .getNftOrderItemById(item.id.decimalStringValue, null)
            .awaitFirst()!!

        assertThat(result.meta).isNull()
        assertItemAndDtoEquals(item, result)
    }

    @Test
    fun `get item by id with meta - synced`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val nftItemMeta = randomNftItemMetaDto()
        val bestSellOrder = randomOrderDto(itemId)
        val bestBidOrder = randomOrderDto(itemId)
        val item = randomItem(itemId).copy(
            bestSellOrder = bestSellOrder,
            bestBidOrder = bestBidOrder,
            totalStock = randomBigInt()
        )

        itemRepository.save(item)
        nftItemControllerApiMock.mockGetNftItemMetaById(itemId, nftItemMeta)

        val result = nftOrderItemControllerApi
            .getNftOrderItemById(item.id.decimalStringValue, true)
            .awaitFirst()!!

        assertThat(result.bestSellOrder).isEqualTo(bestSellOrder)
        assertThat(result.bestBidOrder).isEqualTo(bestBidOrder)
        assertThat(result.meta).isEqualTo(nftItemMeta)
        assertThat(result.unlockable).isEqualTo(false)
        assertThat(result.totalStock).isEqualTo(item.totalStock)
        assertItemAndDtoEquals(item, result)
    }

    @Test
    fun `get item by id - not found`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val nftApiError = NftIndexerApiErrorDto(404, NftIndexerApiErrorDto.Code.ITEM_NOT_FOUND, "")

        nftItemControllerApiMock.mockGetNftItemById(itemId, nftApiError)

        val ex = assertThrows<ProtocolApiResponseException> {
            nftOrderItemControllerApi
                .getNftOrderItemById(itemId.decimalStringValue, null)
                .block()
        }

        assertThat(ex.responseObject is NftOrderApiErrorDto).isEqualTo(true)
        assertThat((ex.responseObject as NftOrderApiErrorDto).status).isEqualTo(nftApiError.status)
    }

    @Test
    fun `get item by id - unexpected api error`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val nftApiError = NftIndexerApiErrorDto(500, NftIndexerApiErrorDto.Code.UNKNOWN, "")

        nftItemControllerApiMock.mockGetNftItemById(itemId, nftApiError)

        val ex = assertThrows<ProtocolApiResponseException> {
            nftOrderItemControllerApi
                .getNftOrderItemById(itemId.decimalStringValue, null)
                .block()
        }

        assertThat(ex.responseObject is NftOrderApiErrorDto).isEqualTo(true)
        assertThat((ex.responseObject as NftOrderApiErrorDto).status).isEqualTo(nftApiError.status)
    }

    @Test
    fun `get all items`() = runBlocking<Unit> {
        val existingItemId = randomItemId()
        val existingBestBid = randomOrderDto(existingItemId)
        val existingItem = randomItem(existingItemId).copy(bestBidOrder = existingBestBid)
        itemRepository.save(existingItem)

        val fetchedItemId = randomItemId()
        val fetchedItem = randomNftItemDto(fetchedItemId)
        val fetchedExistingItem = randomNftItemDto(existingItemId)

        val continuation = randomString()
        val size = 15
        val showDeleted = true
        val includeMeta = false
        val from = randomLong()
        val to = randomLong()

        // If we have stored item, we should not enrich it despite of it was fetched from API
        nftItemControllerApiMock.mockGetNftAllItems(
            continuation,
            size,
            showDeleted,
            from,
            to,
            includeMeta,
            fetchedExistingItem,
            fetchedItem
        )

        val result = nftOrderItemControllerApi.getNftOrderAllItems(
            continuation, size, showDeleted, from, to, includeMeta
        ).awaitFirstOrNull()!!

        assertThat(result.data.size).isEqualTo(2)
        assertThat(result.data[0].meta).isNull()
        assertThat(result.data[1].meta).isNull()

        assertItemAndDtoEquals(existingItem, result.data[0])
        assertThat(existingBestBid).isEqualTo(result.data[0].bestBidOrder)
        assertThat(result.data[0].unlockable).isEqualTo(false)

        assertItemDtoAndNftDtoEquals(result.data[1], fetchedItem)
    }

    @Test
    fun `get all items with meta`() = runBlocking<Unit> {
        val existingItemId = randomItemId()
        val existingItemMeta = randomNftItemMetaDto()
        val existingBestBid = randomOrderDto(existingItemId)
        val existingItem = randomItem(existingItemId).copy(bestBidOrder = existingBestBid)
        val fetchedExistingItem = randomNftItemDto(existingItemId).copy(meta = existingItemMeta)
        itemRepository.save(existingItem)

        val fetchedItemId = randomItemId()
        val fetchedItemMeta = randomNftItemMetaDto()
        val fetchedItem = randomNftItemDto(fetchedItemId).copy(meta = fetchedItemMeta)

        val continuation = randomString()
        val size = 15
        val showDeleted = true
        val includeMeta = true
        val from = randomLong()
        val to = randomLong()

        // If we have stored item, we should not enrich it despite of it was fetched from API
        nftItemControllerApiMock.mockGetNftAllItems(
            continuation,
            size,
            showDeleted,
            from,
            to,
            includeMeta,
            fetchedExistingItem,
            fetchedItem
        )

        val result = nftOrderItemControllerApi.getNftOrderAllItems(
            continuation, size, showDeleted, from, to, includeMeta
        ).awaitFirstOrNull()!!

        assertThat(result.data.size).isEqualTo(2)

        assertThat(result.data[0].meta).isEqualTo(existingItemMeta)
        assertThat(result.data[1].meta).isEqualTo(fetchedItemMeta)

        assertItemAndDtoEquals(existingItem, result.data[0])
        assertThat(existingBestBid).isEqualTo(result.data[0].bestBidOrder)
        assertThat(result.data[0].unlockable).isEqualTo(false)

        assertItemDtoAndNftDtoEquals(result.data[1], fetchedItem)
    }

    @Test
    fun `get items by owner`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val nftItem = randomNftItemDto(itemId)
        val owner = randomAddress()

        nftItemControllerApiMock.mockGetNftOrderItemsByOwner(owner.hex(), nftItem)

        val result = nftOrderItemControllerApi.getNftOrderItemsByOwner(owner.hex(), null, null, null)
            .awaitFirstOrNull()!!

        assertThat(result.data.size).isEqualTo(1)
        assertItemDtoAndNftDtoEquals(result.data[0], nftItem)
    }

    @Test
    fun `get items by collection`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val nftItem = randomNftItemDto(itemId)
        val bestSell = randomOrderDto(itemId)
        val item = randomItem(itemId).copy(bestSellOrder = bestSell)
        itemRepository.save(item)

        val collection = randomAddress()

        // Items requested, but enrichment data should be taken from existing Item
        nftItemControllerApiMock.mockGetNftOrderItemsByCollection(collection.hex(), nftItem)
        val result = nftOrderItemControllerApi.getNftOrderItemsByCollection(collection.hex(), null, null, null)
            .awaitFirstOrNull()!!

        assertThat(result.data.size).isEqualTo(1)
        assertItemAndDtoEquals(item, result.data[0])
        assertThat(bestSell).isEqualTo(result.data[0].bestSellOrder)
    }

    @Test
    fun `get items by creator`() = runBlocking<Unit> {
        val creator = randomAddress()

        nftItemControllerApiMock.mockGetNftOrderItemsByCreator(creator.hex())

        val result = nftOrderItemControllerApi.getNftOrderItemsByCreator(creator.hex(), null, null, null)
            .awaitFirstOrNull()!!

        assertThat(result.data.size).isEqualTo(0)
    }

    @Test
    fun `get lazy item by id`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val lazyNft = randomLazyErc721Dto(itemId)

        nftItemControllerApiMock.mockGetLazyItemById(itemId.decimalStringValue, lazyNft)

        val result = nftOrderItemControllerApi.getNftOrderLazyItemById(itemId.decimalStringValue)
            .awaitFirstOrNull()!!

        assertThat(lazyNft).isEqualTo(result)
    }

    @Test
    fun `get item meta by id`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val itemMeta = randomNftItemMetaDto()

        nftItemControllerApiMock.mockGetItemMetaById(itemId.decimalStringValue, itemMeta)

        val result = nftOrderItemControllerApi.getNftOrderItemMetaById(itemId.decimalStringValue)
            .awaitFirstOrNull()!!

        assertThat(itemMeta).isEqualTo(result)
    }
}
