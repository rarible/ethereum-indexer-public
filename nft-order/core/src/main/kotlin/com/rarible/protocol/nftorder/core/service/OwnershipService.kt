package com.rarible.protocol.nftorder.core.service

import com.mongodb.client.result.DeleteResult
import com.rarible.core.common.convert
import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.nftorder.core.data.Fetched
import com.rarible.protocol.nftorder.core.data.ItemSellStats
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.nftorder.core.repository.OwnershipRepository
import com.rarible.protocol.nftorder.core.util.spent
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

@Component
class OwnershipService(
    private val conversionService: ConversionService,
    private val nftOwnershipControllerApi: NftOwnershipControllerApi,
    private val ownershipRepository: OwnershipRepository
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

    suspend fun getOrFetchOwnershipById(ownershipId: OwnershipId): Fetched<Ownership> {
        val ownership = get(ownershipId)
        return if (ownership != null) {
            Fetched(ownership, false)
        } else {
            Fetched(fetchOwnership(ownershipId), true)
        }
    }

    private suspend fun fetchOwnership(ownershipId: OwnershipId): Ownership {
        val now = nowMillis()
        val nftOwnershipDto = nftOwnershipControllerApi
            .getNftOwnershipById(ownershipId.stringValue)
            .awaitFirstOrNull()!!

        logger.info("Fetched Ownership by Id [{}] ({}ms)", ownershipId, spent(now))
        return conversionService.convert(nftOwnershipDto)
    }

}