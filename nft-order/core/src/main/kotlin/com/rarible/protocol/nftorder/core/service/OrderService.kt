package com.rarible.protocol.nftorder.core.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderIdsDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.nftorder.core.data.RaribleOrderChecker
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.nftorder.core.model.ShortOrder
import com.rarible.protocol.nftorder.core.util.spent
import com.rarible.protocol.order.api.client.OrderControllerApi
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class OrderService(
    private val orderControllerApi: OrderControllerApi
) {

    private val logger = LoggerFactory.getLogger(OrderService::class.java)

    suspend fun getById(hash: Word): OrderDto? {
        return try {
            orderControllerApi.getOrderByHash(hash.prefixed()).awaitFirstOrNull()
        } catch (e: OrderControllerApi.ErrorGetOrderByHash) {
            logger.warn("Unable to retrieve original Order [{}] from indexer: {}", hash, e.message)
            null
        }
    }

    suspend fun getByIds(ids: List<Word>): List<OrderDto> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        val result = orderControllerApi.getOrdersByIds(OrderIdsDto(ids)).collectList().awaitFirst()
        val notFound = result.map { it.hash }.subtract(ids)
        if (notFound.isNotEmpty()) {
            logger.warn("Orders not found in order-indexer: {}", notFound)
        }
        return result
    }

    suspend fun fetchOrderIfDiffers(existing: ShortOrder?, order: OrderDto?): OrderDto? {
        // Nothing to download - there is no existing short order
        if (existing == null) {
            return null
        }
        // Full order we already fetched is the same as short Order we want to download - using obtained order here
        if (existing.hash == order?.hash) {
            return order
        }
        // Downloading full order in common case
        return getById(existing.hash)
    }

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
        val bestOfAll = fetchApi(clientCall(null))
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
