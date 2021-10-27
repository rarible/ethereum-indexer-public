package com.rarible.protocol.order.core.repository.auction

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Word
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.query.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class AuctionHistoryRepository(
    private val template: ReactiveMongoTemplate
) {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun save(logEvent: LogEvent): Mono<LogEvent> {
        return template.save(logEvent, COLLECTION)
    }

    fun findLogEvents(hash: Word?, from: Word?): Flux<LogEvent> {
        val criteria = when {
            hash != null -> LogEvent::data / AuctionHistory::hash isEqualTo hash
            from != null -> LogEvent::data / AuctionHistory::hash gt from
            else -> Criteria()
        }
        val query = Query(criteria).with(LOG_SORT_ASC)
        return template.find(query, COLLECTION)
    }

    fun findAll(): Flux<LogEvent> {
        return template.findAll(COLLECTION)
    }

    companion object {
        const val COLLECTION = "auction_history"

        val LOG_SORT_ASC: Sort = Sort
            .by(
                "${LogEvent::data.name}.${AuctionHistory::hash.name}",
                LogEvent::blockNumber.name,
                LogEvent::logIndex.name,
                LogEvent::minorLogIndex.name
            )
    }
}
