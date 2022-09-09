package com.rarible.protocol.order.core.repository.pool

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.model.PoolNftChange
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
import scalether.domain.Address

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

    suspend fun getLatestPoolNftChange(collection: Address, tokenId: EthUInt256): List<LogEvent> {
        val criteria = Criteria().andOperator(
            LogEvent::data / PoolNftChange::collection isEqualTo collection,
            LogEvent::data / PoolNftChange::tokenIds isEqualTo tokenId,
            LogEvent::status  isEqualTo LogEventStatus.CONFIRMED,
        )
        val query = Query(criteria)
            .with(POOL_NFT_CHANGE_SORT_DESC)
            .withHint(Indexes.NFT_CHANGES_POOL_ITEM_IDS_DEFINITION.indexKeys)
            .limit(1)

        return template.find<LogEvent>(query, COLLECTION).collectList().awaitFirst()
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

        val NFT_CHANGES_POOL_ITEM_IDS_DEFINITION: Index = Index()
            .on("${LogEvent::data.name}.${PoolNftChange::collection.name}", Sort.Direction.ASC)
            .on("${LogEvent::data.name}.${PoolNftChange::tokenIds.name}", Sort.Direction.ASC)
            .on(LogEvent::blockNumber.name, Sort.Direction.ASC)
            .on(LogEvent::logIndex.name, Sort.Direction.ASC)
            .on(LogEvent::minorLogIndex.name, Sort.Direction.ASC)
            .background()

        val ALL_INDEXES = listOf(
            HASH_DEFINITION,
            BY_UPDATED_AT_FIELD,
            NFT_CHANGES_POOL_ITEM_IDS_DEFINITION
        )
    }

    companion object {
        const val COLLECTION = "pool_history"

        val POOL_NFT_CHANGE_SORT_DESC: Sort = Sort
            .by(
                Sort.Direction.DESC,
                "${LogEvent::data.name}.${PoolNftChange::collection.name}",
                "${LogEvent::data.name}.${PoolNftChange::tokenIds.name}",
                LogEvent::blockNumber.name,
                LogEvent::logIndex.name,
                LogEvent::minorLogIndex.name
            )

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
