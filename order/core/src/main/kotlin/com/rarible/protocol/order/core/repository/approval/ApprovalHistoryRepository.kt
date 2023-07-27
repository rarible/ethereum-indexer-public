package com.rarible.protocol.order.core.repository.approval

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.ApprovalHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun save(logEvent: ReversedEthereumLogRecord): EthereumLogRecord {
        return template.save(logEvent, COLLECTION).awaitSingle()
    }

    fun findAll(): Flow<ReversedEthereumLogRecord> {
        return template.findAll(ReversedEthereumLogRecord::class.java, COLLECTION).asFlow()
    }

    suspend fun lastApprovalLogEvent(collection: Address, owner: Address, operator: Address): ReversedEthereumLogRecord? {
        val criteria: Criteria = where(ReversedEthereumLogRecord::data / ApprovalHistory::collection).isEqualTo(collection)
            .and(ReversedEthereumLogRecord::data / ApprovalHistory::owner).isEqualTo(owner)
            .and(ReversedEthereumLogRecord::data / ApprovalHistory::operator).isEqualTo(operator)
            .and(ReversedEthereumLogRecord::status).isEqualTo(EthereumBlockStatus.CONFIRMED)

        val query = Query.query(criteria)
        query.with(Sort.by(
            Sort.Direction.DESC,
            ReversedEthereumLogRecord::blockNumber.name,
            ReversedEthereumLogRecord::logIndex.name,
            ReversedEthereumLogRecord::minorLogIndex.name
        ))
        return template.findOne<ReversedEthereumLogRecord>(query, COLLECTION).awaitFirstOrNull()
    }

    companion object {
        const val COLLECTION = "approve_history"
    }
}
