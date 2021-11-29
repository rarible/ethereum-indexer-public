package com.rarible.protocol.nft.core.repository.history

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.listener.log.domain.LogEvent
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address

@Component
@CaptureSpan(type = SpanType.DB)
class NftHistoryRepository(
    private val mongo: ReactiveMongoOperations
) {
    private val allIndexes = listOf(
        Index()
            .on("${LogEvent::data.name}._id", Sort.Direction.ASC)
            .on(LogEvent::blockNumber.name, Sort.Direction.ASC)
            .on(LogEvent::logIndex.name, Sort.Direction.ASC)
            .on(LogEvent::minorLogIndex.name, Sort.Direction.ASC)
            .background()
    )

    suspend fun createIndexes() {
        allIndexes.forEach { index ->
            mongo.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    fun findAllByCollection(address: Address?): Flux<LogEvent> {
        val criteria = if (address != null) {
            Criteria.where("${LogEvent::data.name}._id").`is`(address)
        } else {
            Criteria()
        }
        return mongo.find(Query(criteria).with(LOG_SORT_ASC), COLLECTION)
    }

    fun save(logEvent: LogEvent): Mono<LogEvent> = mongo.save(logEvent, COLLECTION)

    companion object {
        const val COLLECTION = "nft_history"

        private val LOG_SORT_ASC = Sort.by(
            "${LogEvent::data.name}._id",
            LogEvent::blockNumber.name,
            LogEvent::logIndex.name,
            LogEvent::minorLogIndex.name
        )
    }
}
