package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.nft.core.model.CollectionOwnershipTransferred
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

@ChangeLog(order = "00019")
class ChangeLog00019UpdateCollectionOwners {
    @ChangeSet(
        id = "ChangeLog00019UpdateCollectionOwners.updateCollectionOwners",
        order = "1",
        author = "protocol",
        runAlways = true //TODO: disable it after release
    )
    fun updateCollectionOwners(
        @NonLockGuarded nftHistoryRepository: NftHistoryRepository,
        @NonLockGuarded tokenRepository: TokenRepository
    ) = runBlocking {
        if (LocalDate.now() > LocalDate.of(2021, 11, 15)) {
            error("Disable this migration by setting runAlways = false!")
        }
        logger.info("Started updating collection owners")
        var seen = 0
        nftHistoryRepository.findAllByCollection(null).collect { logEvent ->
            seen++
            val ownershipTransfer = logEvent.data as? CollectionOwnershipTransferred ?: return@collect
            try {
                optimisticLock {
                    val token = tokenRepository.findById(ownershipTransfer.id).awaitFirstOrNull()
                    if (token != null && token.owner != ownershipTransfer.newOwner) {
                        logger.info("Updating owner of ${ownershipTransfer.id} from ${token.owner} to ${ownershipTransfer.newOwner} (seen $seen)")
                        tokenRepository.save(token.copy(owner = ownershipTransfer.newOwner)).awaitFirstOrNull()
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to update owner of collection ${ownershipTransfer.id}", e)
            }
        }
        logger.info("Finished updating collection owners")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00019UpdateCollectionOwners::class.java)
    }
}
