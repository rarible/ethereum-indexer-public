package com.rarible.protocol.nft.core.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.nft.core.model.InconsistentItem
import com.rarible.protocol.nft.core.model.InconsistentItem.Companion.COLLECTION
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.CriteriaDefinition
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@CaptureSpan(type = SpanType.DB)
class InconsistentItemRepository(
    private val mongo: ReactiveMongoOperations
) {
    suspend fun dropCollection() {
        mongo.dropCollection(COLLECTION).awaitFirstOrNull()
    }

    /**
     * Returns true if item was not in the collection before
     */
    suspend fun save(inconsistentItem: InconsistentItem): Boolean {
        return try {
            mongo.insert(inconsistentItem, COLLECTION).awaitFirstOrNull()
            true
        } catch (e: DuplicateKeyException) {
            false
        }
    }

    suspend fun get(id: ItemId): InconsistentItem? {
        return mongo.findById<InconsistentItem>(id, COLLECTION).awaitFirstOrNull()
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
}
