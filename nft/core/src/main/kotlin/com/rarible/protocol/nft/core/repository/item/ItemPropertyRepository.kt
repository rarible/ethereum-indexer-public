package com.rarible.protocol.nft.core.repository.item

import com.rarible.protocol.nft.core.model.ItemId
import org.bson.Document
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration

class ItemPropertyRepository(
    private val mongo: ReactiveMongoOperations,
    private val clock: Clock
) {
    fun save(itemId: ItemId, properties: String): Mono<Boolean> {
        val criteria = Criteria.where("_id").isEqualTo(itemId.stringValue)

        val update = Update().apply {
            set(PROPERTIES_FILED, properties)
            set(UPDATED_AT_FILED, clock.instant())
        }
        return mongo
            .upsert(Query(criteria), update, COLLECTION)
            .map { it.wasAcknowledged() }
    }

    fun get(itemId: ItemId, timeToLive: Duration): Mono<String> {
        val minUpdatedAt = clock.instant() - timeToLive

        val query = Query().apply {
            addCriteria(Criteria.where("_id").isEqualTo(itemId.stringValue))
            addCriteria(Criteria.where(UPDATED_AT_FILED).gt(minUpdatedAt))
        }
        return mongo
            .findOne<Document>(query, COLLECTION)
            .map { it.getString(PROPERTIES_FILED) }
    }

    private companion object {
        const val COLLECTION = "item_properties"
        const val PROPERTIES_FILED = "properties"
        const val UPDATED_AT_FILED = "updated_at"
    }
}
