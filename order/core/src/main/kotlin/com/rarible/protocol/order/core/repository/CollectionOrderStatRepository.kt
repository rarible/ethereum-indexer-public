package com.rarible.protocol.order.core.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.nowMillis
import com.rarible.protocol.order.core.model.CollectionOrderStat
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.lt
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.time.Duration

@Component
@CaptureSpan(type = SpanType.DB)
class CollectionOrderStatRepository(
    private val mongo: ReactiveMongoOperations
) {

    suspend fun createIndexes() {
        ALL_INDEXES.forEach { index ->
            mongo.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun save(token: CollectionOrderStat): CollectionOrderStat {
        return mongo.save(token).awaitFirst()
    }

    suspend fun get(token: Address): CollectionOrderStat? {
        return mongo.findById(token, CollectionOrderStat::class.java).awaitFirstOrNull()
    }

    suspend fun findOld(limit: Int, timeOffset: Duration): List<CollectionOrderStat> {
        val from = nowMillis().minus(timeOffset)
        val query = Query(CollectionOrderStat::lastUpdatedAt lt from)
            .with(Sort.by(Sort.Direction.ASC, CollectionOrderStat::lastUpdatedAt.name))
            .limit(limit)

        return mongo.find(query, CollectionOrderStat::class.java).collectList().awaitFirst()
    }

    companion object {

        private val COLLECTION = "collection_stat"

        private val BY_LAST_UPDATED: Index = Index()
            .on(CollectionOrderStat::lastUpdatedAt.name, Sort.Direction.ASC)
            .background()

        val ALL_INDEXES = listOf(
            BY_LAST_UPDATED
        )
    }

}