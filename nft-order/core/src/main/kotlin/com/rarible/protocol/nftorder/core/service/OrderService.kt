package com.rarible.protocol.nftorder.core.service

import com.rarible.protocol.dto.*
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
        return withPreferredRariblePlatform { platform ->
            orderControllerApi.getSellOrdersByItem(
                itemId.token.hex(),
                itemId.tokenId.value.toString(),
                null,
                null,
                platform,
                null,
                1
            )
        }
    }

    suspend fun getBestSell(id: OwnershipId): OrderDto? {
        logger.info("Fetching best sell order for Ownership [{}]", id)
        return withPreferredRariblePlatform { platform ->
            orderControllerApi.getSellOrdersByItem(
                id.token.hex(),
                id.tokenId.value.toString(),
                id.owner.hex(),
                null,
                platform,
                null,
                1
            )
        }
    }

    suspend fun getBestBid(itemId: ItemId): OrderDto? {
        logger.info("Fetching best bid order for Item [{}]", itemId)
        return withPreferredRariblePlatform { platform ->
            orderControllerApi.getOrderBidsByItem(
                itemId.token.hex(),
                itemId.tokenId.value.toString(),
                null,
                null,
                platform,
                null,
                1
            )
        }
    }

    private suspend fun withPreferredRariblePlatform(
        clientCall: suspend (platform: PlatformDto) -> Mono<OrdersPaginationDto>
    ): OrderDto? {
        val bestOfAll = fetchApi(clientCall(PlatformDto.ALL))
        logger.debug("Found best order from ALL platforms: [{}]", bestOfAll)
        if (bestOfAll == null || isRaribleOrder(bestOfAll)) {
            return bestOfAll
        }
        logger.debug("Order [{}] is not a preferred platform order, checking preferred platform...", bestOfAll)
        val preferredPlatformBestOrder = fetchApi(clientCall(PlatformDto.RARIBLE))
        logger.debug("Checked preferred platform for best order: [{}]")
        return preferredPlatformBestOrder ?: bestOfAll
    }

    private fun isRaribleOrder(order: OrderDto): Boolean {
        return order is RaribleV2OrderDto || order is LegacyOrderDto
    }

    private suspend fun fetchApi(call: Mono<OrdersPaginationDto>): OrderDto? {
        val dto = call.awaitFirstOrDefault(OrdersPaginationDto(emptyList(), null))
        return dto.orders.firstOrNull()
    }
}