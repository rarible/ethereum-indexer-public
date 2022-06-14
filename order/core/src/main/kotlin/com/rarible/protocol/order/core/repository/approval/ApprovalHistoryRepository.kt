package com.rarible.protocol.order.core.repository.approval

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.ApprovalHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
@CaptureSpan(type = SpanType.DB)
class ApprovalHistoryRepository(
    private val template: ReactiveMongoTemplate
) {

    suspend fun save(logEvent: LogEvent): LogEvent {
        return template.save(logEvent, COLLECTION).awaitSingle()
    }

    fun findAll(): Flow<LogEvent> {
        return template.findAll(LogEvent::class.java, COLLECTION).asFlow()
    }

    suspend fun lastApprovalLogEvent(collection: Address, owner: Address): LogEvent? {
        val criteria: Criteria = where(LogEvent::data / ApprovalHistory::collection).isEqualTo(collection)
            .and(LogEvent::data / ApprovalHistory::owner).isEqualTo(owner)
            .and(LogEvent::status).isEqualTo(LogEventStatus.CONFIRMED)

        val query = Query.query(criteria)
        query.with(Sort.by(
            Sort.Direction.DESC,
            LogEvent::blockNumber.name,
            LogEvent::logIndex.name,
            LogEvent::minorLogIndex.name
        ))
        return template.findOne<LogEvent>(query, COLLECTION).awaitFirstOrNull()
    }

    companion object {
        const val COLLECTION = "approve_history"
    }
}
