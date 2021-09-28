package com.rarible.protocol.nftorder.api.test.mock

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderIdsDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.order.api.client.OrderControllerApi
import io.daonomic.rpc.domain.Word
import io.mockk.every
import org.springframework.web.reactive.function.client.WebClientResponseException
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

    fun mockGetByIdNotFound(vararg hashes: Word) {
        hashes.forEach {
            every {
                orderControllerApi.getOrderByHash(it.prefixed())
            } throws OrderControllerApi.ErrorGetOrderByHash(WebClientResponseException(404, "", null, null, null))
        }
    }

    fun mockGetByIds(vararg orders: OrderDto) {
        val hashes = orders.map { it.hash }
        every {
            orderControllerApi.getOrdersByIds(OrderIdsDto(hashes.toList()))
        } returns orders.toFlux()
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

    fun mockGetSellOrdersByOwnership(ownershipId: OwnershipId, vararg returnOrders: OrderDto) {
        every {
            orderControllerApi.getSellOrdersByItem(
                ownershipId.token.hex(),
                ownershipId.tokenId.value.toString(),
                ownershipId.owner.hex(),
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