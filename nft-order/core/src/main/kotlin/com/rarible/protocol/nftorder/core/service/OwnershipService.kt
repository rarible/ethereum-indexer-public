package com.rarible.protocol.nftorder.core.service

import com.mongodb.client.result.DeleteResult
import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.NftOrderOwnershipDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.nftorder.core.converter.NftOwnershipDtoConverter
import com.rarible.protocol.nftorder.core.converter.OwnershipToDtoConverter
import com.rarible.protocol.nftorder.core.data.Fetched
import com.rarible.protocol.nftorder.core.data.ItemSellStats
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.nftorder.core.repository.OwnershipRepository
import com.rarible.protocol.nftorder.core.util.spent
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OwnershipService(
    private val nftOwnershipControllerApi: NftOwnershipControllerApi,
    private val ownershipRepository: OwnershipRepository,
    private val orderService: OrderService
) {

    private val logger = LoggerFactory.getLogger(OwnershipService::class.java)

    suspend fun get(ownershipId: OwnershipId): Ownership? {
        return ownershipRepository.get(ownershipId)
    }

    suspend fun save(ownership: Ownership): Ownership {
        return ownershipRepository.save(ownership)
    }

    suspend fun delete(ownershipId: OwnershipId): DeleteResult? {
        val result = ownershipRepository.delete(ownershipId)
        logger.debug("Deleted Ownership [{}], deleted: {}", ownershipId, result?.deletedCount)
        return result
    }

    suspend fun findAll(ids: List<OwnershipId>): List<Ownership> {
        return ownershipRepository.findAll(ids)
    }

    suspend fun getItemSellStats(itemId: ItemId): ItemSellStats {
        val now = nowMillis()
        val result = ownershipRepository.getItemSellStats(itemId)
        logger.info("SellStat query executed for ItemId [{}]: [{}] ({}ms)", itemId, result, spent(now))
        return result
    }

    suspend fun getOrFetchOwnershipById(ownershipId: OwnershipId): Fetched<Ownership, NftOwnershipDto> {
        val ownership = get(ownershipId)
        return if (ownership != null) {
            Fetched(ownership, null)
        } else {
            val now = nowMillis()
            val nftOwnershipDto = nftOwnershipControllerApi
                .getNftOwnershipById(ownershipId.stringValue)
                .awaitFirstOrNull()!!

            logger.info("Fetched Ownership by Id [{}] ({}ms)", ownershipId, spent(now))
            val fetchedOwnership = NftOwnershipDtoConverter.convert(nftOwnershipDto)
            Fetched(fetchedOwnership, nftOwnershipDto)
        }
    }

    suspend fun fetchAllByItemId(itemId: ItemId): List<NftOwnershipDto> {
        var continuation: String? = null
        val result = ArrayList<NftOwnershipDto>()
        do {
            val page = nftOwnershipControllerApi.getNftOwnershipsByItem(
                itemId.token.hex(),
                itemId.tokenId.value.toString(),
                continuation,
                null
            ).awaitFirst()
            result.addAll(page.ownerships)
            continuation = page.continuation
        } while (continuation != null)
        return result
    }

    suspend fun enrichOwnership(ownership: Ownership, order: OrderDto? = null): NftOrderOwnershipDto {
        val bestSellOrder = orderService.fetchOrderIfDiffers(ownership.bestSellOrder, order)

        val orders = listOfNotNull(bestSellOrder)
            .associateBy { it.hash }

        return OwnershipToDtoConverter.convert(ownership, orders)
    }

}