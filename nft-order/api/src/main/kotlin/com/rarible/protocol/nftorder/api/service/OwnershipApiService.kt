package com.rarible.protocol.nftorder.api.service

import com.rarible.core.common.convert
import com.rarible.protocol.dto.NftOrderOwnershipDto
import com.rarible.protocol.dto.NftOwnershipsDto
import com.rarible.protocol.dto.PageNftOrderOwnershipItemDto
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.nftorder.core.service.OwnershipService
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class OwnershipApiService(
    private val conversionService: ConversionService,
    private val nftOwnershipControllerApi: NftOwnershipControllerApi,
    private val ownershipService: OwnershipService
) {

    private val logger = LoggerFactory.getLogger(OwnershipApiService::class.java)

    suspend fun getOwnershipById(id: OwnershipId): NftOrderOwnershipDto {
        logger.debug("Get Ownership: [{}]", id)
        val ownership = ownershipService.getOrFetchEnrichedOwnershipById(id).entity
        return conversionService.convert(ownership)
    }

    suspend fun getAllOwnerships(continuation: String?, size: Int?): PageNftOrderOwnershipItemDto {
        logger.debug("Get all Ownerships with params: continuation={}, size={}", continuation, size)
        return ownershipsResponse(nftOwnershipControllerApi.getNftAllOwnerships(continuation, size))
    }

    suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?
    ): PageNftOrderOwnershipItemDto {
        logger.debug(
            "Get Ownerships by item with params: contract=[{}], tokenId=[{}], continuation={}, size={}",
            contract, tokenId, continuation, size
        )
        return ownershipsResponse(
            nftOwnershipControllerApi.getNftOwnershipsByItem(contract, tokenId, continuation, size)
        )
    }

    private suspend fun ownershipsResponse(apiResponse: Mono<NftOwnershipsDto>): PageNftOrderOwnershipItemDto {
        val response = apiResponse.awaitFirst()
        val ownerships = response.ownerships

        return if (ownerships.isEmpty()) {
            logger.debug("No Ownerships found")
            PageNftOrderOwnershipItemDto(null, emptyList())
        } else {
            val existingOwnerships: Map<OwnershipId, Ownership> = ownershipService
                .findAll(ownerships.map { OwnershipId.parseId(it.id) })
                .associateBy { it.id }
            logger.debug("{} enriched of {} Items found in DB", existingOwnerships.size, ownerships.size)

            val result = ownerships.map {
                val ownershipId = OwnershipId.parseId(it.id)
                // Nothing to enrich, taking item we got from Nft-Indexer
                val ownership = existingOwnerships[ownershipId] ?: conversionService.convert(it)
                conversionService.convert<NftOrderOwnershipDto>(ownership)
            }

            PageNftOrderOwnershipItemDto(response.continuation, result)
        }
    }
}