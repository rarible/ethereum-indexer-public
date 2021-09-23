package com.rarible.protocol.nftorder.api.test.mock

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderIdsDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.order.api.client.OrderControllerApi
import io.mockk.every
import reactor.core.publisher.Mono

class OrderControllerApiMock(
    private val orderControllerApi: OrderControllerApi
) {

    fun mockGetByIds(vararg orders: OrderDto) {
        every {
            orderControllerApi.getOrdersByIds(OrderIdsDto(hashes.toList()))
        }
    }

    fun mockGetSellOrdersByItem(itemId: ItemId, vararg returnOrders: OrderDto) {
        every {
            orderControllerApi.getSellOrdersByItem(
                itemId.token.hex(),
                itemId.tokenId.value.toString(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Mono.just(OrdersPaginationDto(returnOrders.asList(), null))
    }

    fun mockGetSellOrdersByItem(itemId: OwnershipId, vararg returnOrders: OrderDto) {
        every {
            orderControllerApi.getSellOrdersByItem(
                itemId.token.hex(),
                itemId.tokenId.value.toString(),
                itemId.owner.hex(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Mono.just(OrdersPaginationDto(returnOrders.asList(), null))
    }

    fun mockGetBidOrdersByItem(itemId: ItemId, vararg returnOrders: OrderDto) {
        every {
            orderControllerApi.getOrderBidsByItem(
                itemId.token.hex(),
                itemId.tokenId.value.toString(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Mono.just(OrdersPaginationDto(returnOrders.asList(), null))
    }

}