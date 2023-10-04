package com.rarible.protocol.nft.core.repository

import com.rarible.protocol.nft.core.model.InconsistentItem
import com.rarible.protocol.nft.core.model.InconsistentItem.Companion.COLLECTION
import com.rarible.protocol.nft.core.model.InconsistentItemStatus
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.InconsistentItemRepository.Indexes.ALL_INDEXES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component

@Component
class InconsistentItemRepository(
    private val mongo: ReactiveMongoOperations
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun createIndexes() {
        ALL_INDEXES.forEach { index ->
            mongo.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun dropCollection() {
        mongo.dropCollection(COLLECTION).awaitFirstOrNull()
    }

    /**
     * Returns true if item was not in the collection before, or was fixed previously
     */
    suspend fun insert(inconsistentItem: InconsistentItem): Boolean {
        return try {
            val previous = get(inconsistentItem.id)
            if (previous == null) {
                mongo.insert(inconsistentItem, COLLECTION).awaitFirstOrNull()
                return true
            } else {
                if (previous.status == InconsistentItemStatus.FIXED) {
                    logger.info("InconsistentItem ${inconsistentItem.id} was fixed previously, " +
                            "but again became inconsistent: $inconsistentItem")
                    mongo.save(
                        inconsistentItem.copy(
                            status = InconsistentItemStatus.RELAPSED,
                            relapseCount = previous.relapseCount?.plus(1)
                        ),
                        COLLECTION
                    ).awaitFirstOrNull()
                    return true
                }
                return false
            }
        } catch (e: DuplicateKeyException) {
            false
        }
    }

    suspend fun save(inconsistentItem: InconsistentItem) {
        mongo.save(inconsistentItem, COLLECTION).awaitFirstOrNull()
    }

    suspend fun get(id: ItemId): InconsistentItem? {
        return mongo.findById<InconsistentItem>(id, COLLECTION).awaitFirstOrNull()
    }

    suspend fun search(query: Query): List<InconsistentItem> {
        return mongo.query<InconsistentItem>()
            .matching(query)
            .all()
            .collectList()
            .awaitFirst()
    }

    suspend fun searchByIds(ids: Set<ItemId>): List<InconsistentItem> {
        val query = Query(Criteria.where("_id").`in`(ids))
        return mongo.query<InconsistentItem>().matching(query)
            .all()
            .collectList()
            .awaitFirst()
    }

    fun findAll(): Flow<InconsistentItem> {
        return mongo.findAll(InconsistentItem::class.java, COLLECTION).asFlow()
    }

    private object Indexes {
        val FOR_ALL_DEFINITION: Index = Index()
            .on(InconsistentItem::lastUpdatedAt.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        val ALL_INDEXES = listOf(FOR_ALL_DEFINITION)
    }
}
