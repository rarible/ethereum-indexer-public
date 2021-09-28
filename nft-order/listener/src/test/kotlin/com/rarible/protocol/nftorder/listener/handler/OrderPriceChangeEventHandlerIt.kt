package com.rarible.protocol.nftorder.listener.handler

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.core.converter.ShortOrderConverter
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.service.ItemService
import com.rarible.protocol.nftorder.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nftorder.listener.test.IntegrationTest
import com.rarible.protocol.nftorder.listener.test.data.*
import org.assertj.core.api.Assertions.assertThat
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

    @Autowired
    lateinit var orderEventHandler: OrderEventHandler

    @Test
    fun `sell order force use from change price event`() = runWithKafka<Unit> {
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

        val currentBestSellOrder = randomLegacyOrderDto(
            randomAssetErc721(makeItemId), ownershipId.owner, randomAssetErc1155(takeItemId)
        ).copy(makePriceUsd = BigDecimal.valueOf(0), takePriceUsd = null)

        orderEventHandler.handle(createOrderUpdateEvent(currentBestSellOrder))

        val item = itemService.get(makeItemId)
        assertThat(item!!.bestSellOrder).isEqualTo(ShortOrderConverter.convert(currentBestSellOrder))

        val updatedOrder1 = randomLegacyOrderDto(
            randomAssetErc721(makeItemId), ownershipId.owner, randomAssetErc1155(takeItemId)
        ).copy(makePriceUsd = BigDecimal.valueOf(10), takePriceUsd = null)

        val updatedOrder2 = randomLegacyOrderDto(
            randomAssetErc721(makeItemId), ownershipId.owner, randomAssetErc1155(takeItemId)
        ).copy(makePriceUsd = BigDecimal.valueOf(1), takePriceUsd = null, cancelled = true) //Dead order

        val updatedOrder3 = randomLegacyOrderDto(
            randomAssetErc721(makeItemId), ownershipId.owner, randomAssetErc1155(takeItemId)
        ).copy(makePriceUsd = BigDecimal.valueOf(11), takePriceUsd = null)

        val updatedOrder4 = randomLegacyOrderDto(
            randomAssetErc721(makeItemId), ownershipId.owner, randomAssetErc1155(takeItemId)
        ).copy(
            makePriceUsd = BigDecimal.valueOf(1),
            takePriceUsd = null,
            makeStock = BigInteger.ZERO
        ) // Dead order

        val updatedOrder5 = randomLegacyOrderDto(
            randomAssetErc721(makeItemId), ownershipId.owner, randomAssetErc1155(takeItemId)
        ).copy(makePriceUsd = BigDecimal.valueOf(2), takePriceUsd = null)

        val updatedOrder6 = randomLegacyOrderDto(
            randomAssetErc721(makeItemId), ownershipId.owner, randomAssetErc1155(takeItemId)
        ).copy(makePriceUsd = BigDecimal.valueOf(1), takePriceUsd = null)

        val updatedOrder7 = randomLegacyOrderDto(
            randomAssetErc721(makeItemId), ownershipId.owner, randomAssetErc1155(takeItemId)
        ).copy(makePriceUsd = BigDecimal.valueOf(32), takePriceUsd = null)

        orderPriceChangeEventHandler.handle(
            createNftSellOrdersPriceUpdateEventDto(
                makeItemId,
                listOf(
                    updatedOrder1,
                    updatedOrder2,
                    updatedOrder3,
                    updatedOrder4,
                    updatedOrder5,
                    updatedOrder6,
                    updatedOrder7
                )
            )
        )

        val makeItem = itemService.get(makeItemId)!!
        assertThat(makeItem.bestSellOrder).isEqualTo(ShortOrderConverter.convert(updatedOrder6))
        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            assertThat(itemEvents).hasSize(2)
        }
    }

    @Test
    fun `bid order use from change price event`() = runWithKafka<Unit> {
        val takeItemId = randomItemId()
        val ownershipId = randomOwnershipId(takeItemId)

        val nftTakeItemDto = randomNftItemDto(takeItemId, randomPartDto(ownershipId.owner))
        val nftOwnership = randomNftOwnershipDto(nftTakeItemDto)

        nftItemControllerApiMock.mockGetNftItemById(takeItemId, nftTakeItemDto)
        nftOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, nftOwnership)

        val currentBestBidOrder = randomLegacyOrderDto(
            randomAssetErc20(), ownershipId.owner, randomAssetErc721(takeItemId)
        ).copy(takePriceUsd = BigDecimal.valueOf(1000), makePriceUsd = null)

        orderEventHandler.handle(createOrderUpdateEvent(currentBestBidOrder))
        val item = itemService.get(takeItemId)

        assertThat(item!!.bestBidOrder).isEqualTo(ShortOrderConverter.convert(currentBestBidOrder))

        val updatedOrder1 = randomLegacyOrderDto(
            randomAssetErc20(), ownershipId.owner, randomAssetErc721(takeItemId)
        ).copy(takePriceUsd = BigDecimal.valueOf(10), makePriceUsd = null)

        val updatedOrder2 = randomLegacyOrderDto(
            randomAssetErc20(), ownershipId.owner, randomAssetErc721(takeItemId)
        ).copy(takePriceUsd = BigDecimal.valueOf(35), makePriceUsd = null, cancelled = true) //Dead order

        val updatedOrder3 = randomLegacyOrderDto(
            randomAssetErc20(), ownershipId.owner, randomAssetErc721(takeItemId)
        ).copy(takePriceUsd = BigDecimal.valueOf(11), makePriceUsd = null)

        val updatedOrder4 = randomLegacyOrderDto(
            randomAssetErc20(), ownershipId.owner, randomAssetErc721(takeItemId)
        ).copy(takePriceUsd = BigDecimal.valueOf(36), makePriceUsd = null, cancelled = true) // Dead order

        val updatedOrder5 = randomLegacyOrderDto(
            randomAssetErc20(), ownershipId.owner, randomAssetErc721(takeItemId)
        ).copy(takePriceUsd = BigDecimal.valueOf(1), makePriceUsd = null)

        val updatedOrder6 = randomLegacyOrderDto(
            randomAssetErc20(), ownershipId.owner, randomAssetErc721(takeItemId)
        ).copy(takePriceUsd = BigDecimal.valueOf(31), makePriceUsd = null)

        val updatedOrder7 = randomLegacyOrderDto(
            randomAssetErc20(), ownershipId.owner, randomAssetErc721(takeItemId)
        ).copy(takePriceUsd = BigDecimal.valueOf(32), makePriceUsd = null)

        orderPriceChangeEventHandler.handle(
            createNftBidOrdersPriceUpdateEventDto(
                takeItemId,
                listOf(
                    updatedOrder1,
                    updatedOrder2,
                    updatedOrder3,
                    updatedOrder4,
                    updatedOrder5,
                    updatedOrder6,
                    updatedOrder7
                )
            )
        )
        val makeItem = itemService.get(takeItemId)!!
        assertThat(makeItem.bestBidOrder).isEqualTo(ShortOrderConverter.convert(updatedOrder7))

        Wait.waitAssert {
            assertThat(itemEvents).hasSize(1)
        }
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

    private fun createOrderUpdateEvent(order: OrderDto): OrderUpdateEventDto {
        return OrderUpdateEventDto(
            randomString(),
            order.hash.hex(),
            order
        )
    }
}
