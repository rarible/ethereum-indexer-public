package com.rarible.protocol.nft.api.service.ownership

import com.rarible.core.common.convert
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.parser.AddressParser
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipContinuation
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipFilterByOwner
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.repository.ownership.OwnershipFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

@Component
class OwnershipApiService(
    private val conversionService: ConversionService,
    private val ownershipRepository: OwnershipRepository
) {
    suspend fun get(ownershipId: OwnershipId, showDeleted: Boolean): NftOwnershipDto {
        return ownershipRepository
            .findById(ownershipId).awaitFirstOrNull()
            ?.takeIf { ownership -> showDeleted || !ownership.deleted }
            ?.let { conversionService.convert<NftOwnershipDto>(it) }
            ?: throw EntityNotFoundApiException("Ownership", ownershipId)
    }

    suspend fun get(ids: List<OwnershipId>): List<NftOwnershipDto> =
        ownershipRepository.findAll(ids)
            .map { conversionService.convert(it) }

    suspend fun getAllByOwner(owner: String): List<Ownership> {
        var continuation: String? = null
        val result = mutableListOf<Ownership>()
        val requestLimit = 1000
        val filter = OwnershipFilterByOwner(OwnershipFilter.Sort.LAST_UPDATE, AddressParser.parse(owner))
        do {
            val ownerships = search(filter, continuation?.let { OwnershipContinuation.parse(it) })
            result.addAll(ownerships)
            val last = if (ownerships.isEmpty() || ownerships.size < requestLimit) null else ownerships.last()
            continuation = last?.let { OwnershipContinuation(it.date, it.id).toString() }
        } while (continuation != null)
        return result
    }

    suspend fun search(
        filter: OwnershipFilter,
        continuation: OwnershipContinuation? = null,
        size: Int? = null
    ): List<Ownership> {
        return ownershipRepository.search(
            filter.toCriteria(continuation, size)
        )
    }
}
