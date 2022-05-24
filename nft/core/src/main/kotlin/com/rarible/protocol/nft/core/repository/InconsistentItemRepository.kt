package com.rarible.protocol.nft.core.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.nft.core.model.InconsistentItem
import com.rarible.protocol.nft.core.model.ItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.DB)
class InconsistentItemRepository(
    private val mongo: ReactiveMongoOperations
) {
    suspend fun dropCollection() {
        mongo.dropCollection(COLLECTION).awaitFirstOrNull()
    }

    suspend fun save(inconsistentItem: InconsistentItem): InconsistentItem {
        return mongo.save(inconsistentItem, COLLECTION).awaitFirst()
    }

    suspend fun get(itemId: ItemId): InconsistentItem? {
        return mongo.findById(itemId.stringValue, InconsistentItem::class.java).awaitFirstOrNull()
    }

    fun findAll(): Flow<InconsistentItem> {
        return mongo.findAll(InconsistentItem::class.java, COLLECTION).asFlow()
    }

    companion object {
        const val COLLECTION = "inconsistent_items"
    }
}
