package com.rarible.protocol.nft.core.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.core.model.CollectionStat
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
class CollectionStatRepository(
    private val mongo: ReactiveMongoOperations
) {

    suspend fun createIndexes() {
        ALL_INDEXES.forEach { index ->
            mongo.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun save(token: CollectionStat): CollectionStat {
        return mongo.save(token).awaitFirst()
    }

    suspend fun get(token: Address): CollectionStat? {
        return mongo.findById(token, CollectionStat::class.java).awaitFirstOrNull()
    }

    suspend fun findOld(limit: Int, timeOffset: Duration): List<CollectionStat> {
        val from = nowMillis().minus(timeOffset)
        val query = Query(CollectionStat::lastUpdatedAt lt from)
            .with(Sort.by(Sort.Direction.ASC, CollectionStat::lastUpdatedAt.name))
            .limit(limit)

        return mongo.find(query, CollectionStat::class.java).collectList().awaitFirst()
    }

    companion object {

        private val COLLECTION = "collection_stat"

        private val BY_LAST_UPDATED: Index = Index()
            .on(CollectionStat::lastUpdatedAt.name, Sort.Direction.ASC)
            .background()

        val ALL_INDEXES = listOf(
            BY_LAST_UPDATED
        )
    }

}