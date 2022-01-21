package com.rarible.protocol.nftorder.listener.test.mock

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderIdsDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.order.api.client.OrderControllerApi
import io.mockk.every
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

class OrderControllerApiMock(
    private val orderControllerApi: OrderControllerApi
) {

    fun mockGetById(vararg orders: OrderDto) {
        orders.forEach {
            every {
                orderControllerApi.getOrderByHash(it.hash.prefixed())
            } returns it.toMono()
        }
    }

    fun mockGetByIds(vararg orders: OrderDto) {
        val hashes = orders.map { it.hash }
        every {
            orderControllerApi.getOrdersByIds(OrderIdsDto(hashes.toList()))
        } returns orders.toFlux()
    }

    fun mockGetSellOrdersByItem(itemId: ItemId, vararg returnOrders: OrderDto) {
        mockGetSellOrdersByItem(itemId, null, *returnOrders)
    }

    fun mockGetSellOrdersByItem(itemId: ItemId, platform: PlatformDto, vararg returnOrders: OrderDto) {
        every {
            orderControllerApi.getSellOrdersByItem(
                itemId.token.hex(),
                itemId.tokenId.value.toString(),
                any(),
                any(),
                platform,
                any(),
                any()
            )
        } returns Mono.just(OrdersPaginationDto(returnOrders.asList(), null))
    }

    fun mockGetSellOrdersByItem(ownershipId: OwnershipId, vararg returnOrders: OrderDto) {
        mockGetSellOrdersByItem(ownershipId, null, *returnOrders)
    }

    fun mockGetSellOrdersByItem(ownershipId: OwnershipId, platform: PlatformDto, vararg returnOrders: OrderDto) {
        every {
            orderControllerApi.getSellOrdersByItem(
                ownershipId.token.hex(),
                ownershipId.tokenId.value.toString(),
                ownershipId.owner.hex(),
                any(),
                platform,
                any(),
                any()
            )
        } returns Mono.just(OrdersPaginationDto(returnOrders.asList(), null))
    }

    fun mockGetBidOrdersByItem(itemId: ItemId, vararg returnOrders: OrderDto) {
        mockGetBidOrdersByItem(itemId, null, *returnOrders)
    }

    fun mockGetBidOrdersByItem(itemId: ItemId, platform: PlatformDto, vararg returnOrders: OrderDto) {
        every {
            orderControllerApi.getOrderBidsByItem(
                itemId.token.hex(),
                itemId.tokenId.value.toString(),
                any(),
                any(),
                platform,
                any(),
                any()
            )
        } returns Mono.just(OrdersPaginationDto(returnOrders.asList(), null))
    }

}
