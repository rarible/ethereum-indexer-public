package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@ChangeLog(order = "00027")
class ChangeLog00027RemoveUnusedIndexes {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ChangeSet(
        id = "ChangeLog00027RemoveUnusedIndexes", order = "1", author = "protocol"
    )
    fun removeIndexes(@NonLockGuarded template: ReactiveMongoOperations) = runBlocking<Unit> {

        dropIndex(template, ItemRepository.COLLECTION, "creator_1_date_-1__id_-1")
        dropIndex(template, ItemRepository.COLLECTION, "owners_1_date_-1__id_-1")
        dropIndex(template, ItemRepository.COLLECTION, "token_1_tokenId_1_date_1__id_1")

        dropIndex(template, OwnershipRepository.COLLECTION, "token_1_tokenId_1_owner_1")
    }

    private suspend fun dropIndex(template: ReactiveMongoOperations, collection: String, indexName: String) {
        val exists = template.indexOps(collection).indexInfo.any { it.name == indexName }.awaitFirst()

        if (!exists) {
            logger.warn("We want to drop index $collection.$indexName but it now found. Skipping ...")
            return
        }

        template.indexOps(collection).dropIndex(indexName).awaitFirstOrNull()
    }
}
