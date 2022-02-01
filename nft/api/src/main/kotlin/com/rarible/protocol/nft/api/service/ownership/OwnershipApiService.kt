package com.rarible.protocol.nft.api.service.ownership

import com.rarible.core.common.convert
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.nft.api.domain.OwnershipContinuation
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.api.service.item.OwnershipFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

@Component
class OwnershipApiService(
    private val conversionService: ConversionService,
    private val ownershipRepository: OwnershipRepository
) {
    suspend fun get(ownershipId: OwnershipId): NftOwnershipDto {
        return ownershipRepository
            .findById(ownershipId).awaitFirstOrNull()
            ?.takeUnless { ownership -> ownership.deleted }
            ?.let { conversionService.convert<NftOwnershipDto>(it) }
            ?: throw EntityNotFoundApiException("Ownership", ownershipId)
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
