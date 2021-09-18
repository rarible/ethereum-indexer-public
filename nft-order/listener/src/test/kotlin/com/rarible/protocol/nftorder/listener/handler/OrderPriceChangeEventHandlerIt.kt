package com.rarible.protocol.nftorder.listener.handler

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.service.ItemService
import com.rarible.protocol.nftorder.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nftorder.listener.test.IntegrationTest
import com.rarible.protocol.nftorder.listener.test.data.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.BigInteger

@IntegrationTest
class OrderPriceChangeEventHandlerIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var orderPriceChangeEventHandler: OrderPriceChangeEventHandler

    @Autowired
    lateinit var itemService: ItemService

    @Test
    fun `sell order use from change price event`() = runBlocking<Unit> {
        val makeItemId = randomItemId()
        val takeItemId = randomItemId()
        val ownershipId = randomOwnershipId(makeItemId)

        val nftMakeItemDto = randomNftItemDto(makeItemId, randomPartDto(ownershipId.owner))
        val nftTakeItemDto = randomNftItemDto(takeItemId)
        val nftOwnership = randomNftOwnershipDto(nftMakeItemDto)

        // Since we don't have any data in Mongo, we need to fetch entities without enrichment data via HTTP API
        nftItemControllerApiMock.mockGetNftItemById(makeItemId, nftMakeItemDto)
        nftItemControllerApiMock.mockGetNftItemById(takeItemId, nftTakeItemDto)
        nftOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, nftOwnership)

        val updatedOrder1 = randomLegacyOrderDto(randomAssetErc721(makeItemId), ownershipId.owner, randomAssetErc1155(takeItemId))
            .copy(takePriceUsd = BigDecimal.valueOf(10), makePriceUsd = null)
        val updatedOrder2 = randomLegacyOrderDto(randomAssetErc721(makeItemId), ownershipId.owner, randomAssetErc1155(takeItemId))
            .copy(takePriceUsd = BigDecimal.valueOf(1), makePriceUsd = null, cancelled = true) //Dead order
        val updatedOrder3 = randomLegacyOrderDto(randomAssetErc721(makeItemId), ownershipId.owner, randomAssetErc1155(takeItemId))
            .copy(takePriceUsd = BigDecimal.valueOf(11), makePriceUsd = null)
        val updatedOrder4 = randomLegacyOrderDto(randomAssetErc721(makeItemId), ownershipId.owner, randomAssetErc1155(takeItemId))
            .copy(takePriceUsd = BigDecimal.valueOf(1), makePriceUsd = null, makeStock = BigInteger.ZERO) // Dead order
        val updatedOrder5 = randomLegacyOrderDto(randomAssetErc721(makeItemId), ownershipId.owner, randomAssetErc1155(takeItemId))
            .copy(takePriceUsd = BigDecimal.valueOf(2), makePriceUsd = null)
        val updatedOrder6 = randomLegacyOrderDto(randomAssetErc721(makeItemId), ownershipId.owner, randomAssetErc1155(takeItemId))
            .copy(takePriceUsd = BigDecimal.valueOf(1), makePriceUsd = null)
        val updatedOrder7 = randomLegacyOrderDto(randomAssetErc721(makeItemId), ownershipId.owner, randomAssetErc1155(takeItemId))
            .copy(takePriceUsd = BigDecimal.valueOf(32), makePriceUsd = null)

        orderPriceChangeEventHandler.handle(
            createNftSellOrdersPriceUpdateEventDto(
                makeItemId,
                listOf(updatedOrder1, updatedOrder2, updatedOrder3, updatedOrder4, updatedOrder5, updatedOrder6, updatedOrder7)
            )
        )

        val makeItem = itemService.get(makeItemId)!!
        Assertions.assertThat(makeItem.bestSellOrder).isEqualTo(updatedOrder6)
    }

    @Test
    fun `bid order use from change price event`() = runBlocking<Unit> {
        val takeItemId = randomItemId()
        val ownershipId = randomOwnershipId(takeItemId)

        val nftTaketemDto = randomNftItemDto(takeItemId, randomPartDto(ownershipId.owner))
        val nftOwnership = randomNftOwnershipDto(nftTaketemDto)

        nftItemControllerApiMock.mockGetNftItemById(takeItemId, nftTaketemDto)
        nftOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, nftOwnership)

        val updatedOrder1 = randomLegacyOrderDto(randomAssetErc20(), ownershipId.owner, randomAssetErc721(takeItemId))
            .copy(makePriceUsd = BigDecimal.valueOf(10), takePriceUsd = null)
        val updatedOrder2 = randomLegacyOrderDto(randomAssetErc20(), ownershipId.owner, randomAssetErc721(takeItemId))
            .copy(makePriceUsd = BigDecimal.valueOf(35), takePriceUsd = null, cancelled = true) //Dead order
        val updatedOrder3 = randomLegacyOrderDto(randomAssetErc20(), ownershipId.owner, randomAssetErc721(takeItemId))
            .copy(makePriceUsd = BigDecimal.valueOf(11), takePriceUsd = null)
        val updatedOrder4 = randomLegacyOrderDto(randomAssetErc20(), ownershipId.owner, randomAssetErc721(takeItemId))
            .copy(makePriceUsd = BigDecimal.valueOf(36), takePriceUsd = null, cancelled = true) // Dead order
        val updatedOrder5 = randomLegacyOrderDto(randomAssetErc20(), ownershipId.owner, randomAssetErc721(takeItemId))
            .copy(makePriceUsd = BigDecimal.valueOf(1), takePriceUsd = null)
        val updatedOrder6 = randomLegacyOrderDto(randomAssetErc20(), ownershipId.owner, randomAssetErc721(takeItemId))
            .copy(makePriceUsd = BigDecimal.valueOf(31), takePriceUsd = null)
        val updatedOrder7 = randomLegacyOrderDto(randomAssetErc20(), ownershipId.owner, randomAssetErc721(takeItemId))
            .copy(makePriceUsd = BigDecimal.valueOf(32), takePriceUsd = null)

        orderPriceChangeEventHandler.handle(
            createNftBidOrdersPriceUpdateEventDto(
                takeItemId,
                listOf(updatedOrder1, updatedOrder2, updatedOrder3, updatedOrder4, updatedOrder5, updatedOrder6, updatedOrder7)
            )
        )

        val makeItem = itemService.get(takeItemId)!!
        Assertions.assertThat(makeItem.bestBidOrder).isEqualTo(updatedOrder7)
    }

    private fun createNftSellOrdersPriceUpdateEventDto(itemId: ItemId, orders: List<OrderDto>): NftOrdersPriceUpdateEventDto {
        return NftSellOrdersPriceUpdateEventDto(
            randomString(),
            itemId.token,
            itemId.tokenId.value,
            orders
        )
    }

    private fun createNftBidOrdersPriceUpdateEventDto(itemId: ItemId, orders: List<OrderDto>): NftOrdersPriceUpdateEventDto {
        return NftBidOrdersPriceUpdateEventDto(
            randomString(),
            itemId.token,
            itemId.tokenId.value,
            orders
        )
    }
}
