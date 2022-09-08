package com.rarible.protocol.order.core.repository.pool

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolHistory
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CaptureSpan(type = SpanType.DB)
@Component
class PoolHistoryRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun createIndexes() {
        Indexes.ALL_INDEXES.forEach { index ->
            template.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    fun save(logEvent: LogEvent): Mono<LogEvent> {
        return template.save(logEvent, COLLECTION)
    }

    fun find(query: Query): Flow<LogEvent> {
        return template.find(query,LogEvent::class.java, COLLECTION).asFlow()
    }

    fun findById(id: ObjectId): Mono<LogEvent> {
        return template.findById(id, COLLECTION)
    }

    fun findLogEvents(hash: Word?, from: Word?, platforms: List<HistorySource>? = null): Flux<LogEvent> {
        var criteria = when {
            hash != null -> LogEvent::data / PoolHistory::hash isEqualTo hash
            from != null -> LogEvent::data / PoolHistory::hash gt from
            else -> Criteria()
        }
        if (platforms?.isNotEmpty() == true) {
            criteria = criteria.and(LogEvent::data / PoolHistory::source).inValues(platforms)
        }
        val query = Query(criteria).with(LOG_SORT_ASC)
        return template.find(query, COLLECTION)
    }

    fun findAll(): Flux<LogEvent> {
        return template.findAll(COLLECTION)
    }

    object Indexes {
        private val HASH_DEFINITION: Index = Index()
            .on("${LogEvent::data.name}.${PoolHistory::hash.name}", Sort.Direction.ASC)
            .on(LogEvent::blockNumber.name, Sort.Direction.ASC)
            .on(LogEvent::logIndex.name, Sort.Direction.ASC)
            .on(LogEvent::minorLogIndex.name, Sort.Direction.ASC)
            .background()

        private val BY_UPDATED_AT_FIELD: Index = Index()
            .on(LogEvent::updatedAt.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        val ALL_INDEXES = listOf(
            HASH_DEFINITION,
            BY_UPDATED_AT_FIELD
        )
    }

    companion object {
        const val COLLECTION = "pool_history"

        val LOG_SORT_ASC: Sort = Sort
            .by(
                "${LogEvent::data.name}.${PoolHistory::hash.name}",
                LogEvent::blockNumber.name,
                LogEvent::logIndex.name,
                LogEvent::minorLogIndex.name
            )

        val logger: Logger = LoggerFactory.getLogger(PoolHistoryRepository::class.java)
    }
}
