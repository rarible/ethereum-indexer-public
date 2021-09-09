package com.rarible.protocol.nftorder.core.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.nftorder.core.data.RaribleOrderChecker
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.nftorder.core.util.spent
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
        val now = nowMillis()
        val result = withPreferredRariblePlatform { platform ->
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
        logger.info("Fetched best sell Order for Item [{}]: [{}] ({}ms)", itemId, result?.hash, spent(now))
        return result
    }

    suspend fun getBestSell(id: OwnershipId): OrderDto? {
        val now = nowMillis()
        val result = withPreferredRariblePlatform { platform ->
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
        logger.info("Fetched best sell Order for Ownership [{}]: [{}] ({}ms)", id, result?.hash, spent(now))
        return result
    }

    suspend fun getBestBid(itemId: ItemId): OrderDto? {
        val now = nowMillis()
        val result = withPreferredRariblePlatform { platform ->
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
        logger.info("Fetching best bid Order for Item [{}]: [{}] ({}ms)", itemId, result?.hash, spent(now))
        return result
    }

    private suspend fun withPreferredRariblePlatform(
        clientCall: suspend (platform: PlatformDto) -> Mono<OrdersPaginationDto>
    ): OrderDto? {
        val bestOfAll = fetchApi(clientCall(PlatformDto.ALL))
        logger.debug("Found best order from ALL platforms: [{}]", bestOfAll)
        if (bestOfAll == null || RaribleOrderChecker.isRaribleOrder(bestOfAll)) {
            return bestOfAll
        }
        logger.debug("Order [{}] is not a preferred platform order, checking preferred platform...", bestOfAll)
        val preferredPlatformBestOrder = fetchApi(clientCall(PlatformDto.RARIBLE))
        logger.debug("Checked preferred platform for best order: [{}]")
        return preferredPlatformBestOrder ?: bestOfAll
    }

    private suspend fun fetchApi(call: Mono<OrdersPaginationDto>): OrderDto? {
        val dto = call.awaitFirstOrDefault(OrdersPaginationDto(emptyList(), null))
        return dto.orders.firstOrNull()
    }
}