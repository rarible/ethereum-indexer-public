package com.rarible.protocol.nft.core.service.item.meta.cache

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
class MongoContentCacheStorage(
    private val template: ReactiveMongoTemplate
) : ContentCacheStorage<ImmutableMetaCacheEntry> {

    val collection: String = template.getCollectionName(ImmutableMetaCacheEntry::class.java)

    override suspend fun get(url: String): ImmutableMetaCacheEntry? {
        return template.findById<ImmutableMetaCacheEntry>(url).awaitFirstOrNull()
    }

    override suspend fun save(content: ImmutableMetaCacheEntry) {
        template.save(content).awaitFirst()
    }

    override suspend fun delete(url: String) {
        val criteria = Criteria("_id").isEqualTo(url)
        template.remove(Query(criteria), collection).awaitFirstOrNull()
    }

}
