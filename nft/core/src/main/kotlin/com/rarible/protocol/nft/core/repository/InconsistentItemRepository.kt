package com.rarible.protocol.nft.core.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.nft.core.model.InconsistentItem
import com.rarible.protocol.nft.core.model.ItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findById
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

    fun findAll(): Flow<InconsistentItem> {
        return mongo.findAll(InconsistentItem::class.java, COLLECTION).asFlow()
    }

    companion object {
        const val COLLECTION = "inconsistent_items"
    }
}
