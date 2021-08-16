package com.rarible.protocol.nftorder.core.service

import com.mongodb.client.result.DeleteResult
import com.rarible.core.common.convert
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.nftorder.core.data.Fetched
import com.rarible.protocol.nftorder.core.data.OwnershipEnrichmentData
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.nftorder.core.repository.OwnershipRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class OwnershipService(
    private val conversionService: ConversionService,
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

    suspend fun getOwnershipsTotalStock(itemId: ItemId): BigInteger {
        val start = System.currentTimeMillis()
        val result = ownershipRepository.getTotalStock(itemId)
        logger.debug(
            "Query for totalStock executed for itemId [{}], returned [{}] time spent: {}ms",
            itemId, result, System.currentTimeMillis() - start
        )
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
        val nftOwnershipDto = nftOwnershipControllerApi
            .getNftOwnershipById(ownershipId.stringValue)
            .awaitFirstOrNull()!!

        return enrichDto(nftOwnershipDto)
    }

    suspend fun enrichDto(nftOwnership: NftOwnershipDto): Ownership {
        val ownershipId = OwnershipId(nftOwnership.contract, EthUInt256(nftOwnership.tokenId), nftOwnership.owner)
        val enrichmentData = getEnrichmentData(ownershipId)
        val rawOwnership = conversionService.convert<Ownership>(nftOwnership)
        return enrichOwnership(rawOwnership, enrichmentData)
    }

    suspend fun enrichOwnership(rawOwnership: Ownership, enrichmentData: OwnershipEnrichmentData): Ownership {
        return rawOwnership.copy(
            bestSellOrder = enrichmentData.bestSellOrder
        )
    }

    suspend fun getEnrichmentData(ownershipId: OwnershipId): OwnershipEnrichmentData {
        return OwnershipEnrichmentData(
            bestSellOrder = orderService.getBestSell(ownershipId)
        )
    }
}