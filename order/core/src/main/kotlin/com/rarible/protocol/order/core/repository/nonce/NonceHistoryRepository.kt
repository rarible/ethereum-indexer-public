package com.rarible.protocol.order.core.repository.nonce

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.mongo.util.div
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.repository.nonce.NonceHistoryRepository.Indexes.ALL_INDEXES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import scalether.domain.Address

@CaptureSpan(type = SpanType.DB)
@Component
class NonceHistoryRepository(
    private val template: ReactiveMongoTemplate
) {

    suspend fun createIndexes() {
        ALL_INDEXES.forEach { index ->
            template.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun save(reversedEthereumLogRecord: ReversedEthereumLogRecord): ReversedEthereumLogRecord {
        return template.save(reversedEthereumLogRecord, COLLECTION).awaitFirst()
    }

    suspend fun findLatestNonceHistoryByMaker(maker: Address, address: Address): ReversedEthereumLogRecord? {
        val criteria = (ReversedEthereumLogRecord::data / ChangeNonceHistory::maker isEqualTo maker)
            .and(ReversedEthereumLogRecord::address).isEqualTo(address)
            .and(ReversedEthereumLogRecord::status).isEqualTo(EthereumBlockStatus.CONFIRMED)

        val query = Query(criteria).with(LOG_SORT_DESC)
        return template.findOne(query, ReversedEthereumLogRecord::class.java, COLLECTION).awaitFirstOrNull()
    }

    fun findAll(): Flow<ReversedEthereumLogRecord> {
        return template.findAll<ReversedEthereumLogRecord>(COLLECTION).asFlow()
    }

    object Indexes {
        private val MAKER_DEFINITION: Index = Index()
            .on(ReversedEthereumLogRecord::status.name, Sort.Direction.ASC)
            .on("${ReversedEthereumLogRecord::data.name}.${ChangeNonceHistory::maker.name}", Sort.Direction.ASC)
            .on(ReversedEthereumLogRecord::blockNumber.name, Sort.Direction.ASC)
            .on(ReversedEthereumLogRecord::logIndex.name, Sort.Direction.ASC)
            .on(ReversedEthereumLogRecord::minorLogIndex.name, Sort.Direction.ASC)
            .background()

        val ALL_INDEXES = listOf(
            MAKER_DEFINITION,
        )
    }

    companion object {
        const val COLLECTION = "nonce_history"

        val LOG_SORT_DESC: Sort = Sort
            .by(
                Sort.Direction.DESC,
                "${ReversedEthereumLogRecord::data.name}.${ChangeNonceHistory::maker.name}",
                ReversedEthereumLogRecord::blockNumber.name,
                ReversedEthereumLogRecord::logIndex.name,
                ReversedEthereumLogRecord::minorLogIndex.name
            )

        val logger: Logger = LoggerFactory.getLogger(NonceHistoryRepository::class.java)
    }
}
