package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "00026")
class ChangeLog00026DeleteManuallyCreatedIndices {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ChangeSet(
        id = "ChangeLog00026DeleteManuallyCreatedIndices.dropIndices",
        order = "1",
        author = "protocol",
        runAlways = false
    )
    fun dropIndices(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking {

        dropIndices(
            template, ItemRepository.COLLECTION,
            listOf(
                "owners_1_date_-1__id_-1",
                "creator_1_date_-1__id_-1"
            )
        )

        dropIndices(
            template, "cache_royalty",
            listOf("updateDate_1")
        )

        dropIndices(
            template, LazyNftItemHistoryRepository.COLLECTION,
            listOf("token_1_tokenId_1")
        )

        dropIndices(
            template, Ownership.COLLECTION,
            listOf("token_1_tokenId_1_owner_1")
        )

        dropIndices(
            template, Token.COLLECTION,
            listOf("owner")
        )

        dropIndices(
            template, NftItemHistoryRepository.COLLECTION,
            listOf("data.date_1")
        )
    }

    private suspend fun dropIndices(
        template: ReactiveMongoTemplate,
        collection: String,
        indices: List<String>
    ) {
        val ops = template.indexOps(collection)
        indices.forEach {
            try {
                ops.dropIndex(it).awaitFirst()
            } catch (ex: Exception) {
                logger.error("Drop index '$it' failed for collection '$collection'", ex)
            }
        }
    }
}
