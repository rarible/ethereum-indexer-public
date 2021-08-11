package com.rarible.protocol.nftorder.core.service

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.order.api.client.OrderControllerApi
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class OrderService(
    private val orderControllerApi: OrderControllerApi
) {

    private val logger = LoggerFactory.getLogger(OrderService::class.java)

    suspend fun getBestSell(itemId: ItemId): OrderDto? {
        logger.info("Fetching best sell order for Item [{}]", itemId)
        return fetchApi(
            orderControllerApi.getSellOrdersByItem(
                itemId.token.hex(),
                itemId.tokenId.value.toString(),
                null,
                null,
                PlatformDto.RARIBLE,
                null,
                1
            )
        )
    }

    suspend fun getBestSell(id: OwnershipId): OrderDto? {
        logger.info("Fetching best sell order for Ownership [{}]", id)
        return fetchApi(
            orderControllerApi.getSellOrdersByItem(
                id.token.hex(),
                id.tokenId.value.toString(),
                id.owner.hex(),
                null,
                PlatformDto.RARIBLE,
                null,
                1
            )
        )
    }

    suspend fun getBestBid(itemId: ItemId): OrderDto? {
        logger.info("Fetching best bid order for Item [{}]", itemId)
        return fetchApi(
            orderControllerApi.getOrderBidsByItem(
                itemId.token.hex(),
                itemId.tokenId.value.toString(),
                null,
                null,
                PlatformDto.RARIBLE,
                null,
                1
            )
        )
    }

    suspend fun fetchApi(call: Mono<OrdersPaginationDto>): OrderDto? {
        val dto = call.awaitFirstOrDefault(OrdersPaginationDto(emptyList(), null))
        return dto.orders.firstOrNull()
    }
}