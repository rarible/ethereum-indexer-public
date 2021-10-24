package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.core.common.retryOptimisticLock
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@ChangeLog(order = "00018")
class ChangeLog00018SetRaribleContractField {
    @ChangeSet(
        id = "ChangeLog00018SetRaribleContractField.setRaribleContractField",
        order = "1",
        author = "protocol"
    )
    fun setRaribleContractField(
        @NonLockGuarded nftHistoryRepository: NftHistoryRepository,
        @NonLockGuarded tokenRepository: TokenRepository
    ) = runBlocking {
        logger.info("Started setting 'isRaribleContract' field")
        nftHistoryRepository.findAllByCollection(null).collect { logEvent ->
            val collectionId = (logEvent.data as? CreateCollection)?.id ?: return@collect
            try {
                tokenRepository.findById(collectionId)
                    .map { it.copy(isRaribleContract = true) }
                    .flatMap { tokenRepository.save(it) }
                    .retryOptimisticLock()
                    .awaitFirst()
                logger.info("Set 'isRaribleContract' = true for $collectionId")
            } catch (e: Exception) {
                logger.error("Failed to save 'isRaribleContract' field for $collectionId", e)
            }
        }
        logger.info("Finished setting 'isRaribleContract' field")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00018SetRaribleContractField::class.java)
    }
}
