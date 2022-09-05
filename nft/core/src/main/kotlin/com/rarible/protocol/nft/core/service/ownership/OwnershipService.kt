package com.rarible.protocol.nft.core.service.ownership

import com.mongodb.client.result.UpdateResult
import com.rarible.core.common.nowMillis
import com.rarible.core.common.orNull
import com.rarible.core.common.toOptional
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.model.OwnershipSaveResult
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address

@Service
class OwnershipService(
    private val ownershipRepository: OwnershipRepository
) {

    val logger: Logger = LoggerFactory.getLogger(OwnershipService::class.java)

    fun get(id: OwnershipId): Mono<Ownership> {
        return ownershipRepository.findById(id)
    }

    suspend fun updateStartVersion(id: OwnershipId): UpdateResult? {
        return ownershipRepository.updateStartVersion(id).awaitFirst()
    }

    fun get(token: Address, tokenId: EthUInt256, owner: Address): Mono<Ownership> {
        return get(OwnershipId(token, tokenId, owner))
    }

    suspend fun getAll(ids: Collection<OwnershipId>): List<Ownership> {
        return ownershipRepository.findAll(ids)
    }

    suspend fun save(ownership: Ownership): Ownership {
        logger.info("Saving Ownership ${ownership.id}")
        return saveInternal(ownership).awaitFirst()
    }

    fun saveIfChanged(marker: Marker, ownership: Ownership): Mono<OwnershipSaveResult> {
        return ownershipRepository.findById(ownership.id).toOptional()
            .flatMap { opt ->
                val existOwnership = opt.orNull()
                if (isOwnershipChanged(existOwnership, ownership)) {
                    saveInternal(marker, ownership.copy(lastUpdatedAt = nowMillis()))
                        .map { OwnershipSaveResult(it, true) }
                } else {
                    logger.info(marker, "Ownership ${ownership.id} don't need to be saved")
                    Mono.just(OwnershipSaveResult(ownership, false))
                }
            }
    }

    private fun isOwnershipChanged(existOwnership: Ownership?, updatedOwnership: Ownership): Boolean =
        existOwnership == null || existOwnership != updatedOwnership.copy(lastUpdatedAt = existOwnership.lastUpdatedAt)
            .withCalculatedFields()

    private fun saveInternal(marker: Marker, ownership: Ownership): Mono<Ownership> {
        logger.info(marker, "Saving Ownership ${ownership.id}")
        return saveInternal(ownership)
    }

    private fun saveInternal(ownership: Ownership): Mono<Ownership> {
        return ownershipRepository.save(ownership.withCalculatedFields())
    }
}
