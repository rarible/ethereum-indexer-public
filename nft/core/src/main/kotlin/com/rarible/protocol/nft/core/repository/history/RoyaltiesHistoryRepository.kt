package com.rarible.protocol.nft.core.repository.history

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.misc.div
import com.rarible.protocol.nft.core.model.RoyaltiesEvent
import com.rarible.protocol.nft.core.model.RoyaltiesEventType
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class RoyaltiesHistoryRepository(
    private val mongo: ReactiveMongoOperations
) {
    suspend fun createIndexes() {
        allIndexes.forEach { index ->
            mongo.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun findLastByCollection(address: Address, type: RoyaltiesEventType): ReversedEthereumLogRecord? {
        val criteria = (ReversedEthereumLogRecord::data / RoyaltiesEvent::type isEqualTo type)
                .and(ReversedEthereumLogRecord::data / RoyaltiesEvent::token).isEqualTo(address)

        val query = Query(criteria).with(LATEST_LOG_SORT).limit(1)
        return mongo.find<ReversedEthereumLogRecord>(query, COLLECTION).awaitFirstOrNull()
    }

    suspend fun save(logEvent: ReversedEthereumLogRecord): ReversedEthereumLogRecord {
        return mongo.save(logEvent, COLLECTION).awaitFirst()
    }

    companion object {
        const val COLLECTION = "royalties_history"

        private val LATEST_LOG_SORT = Sort.by(
            Sort.Direction.DESC,
            LogEvent::blockNumber.name,
            LogEvent::logIndex.name,
            LogEvent::minorLogIndex.name,
        )
    }

    private val allIndexes: List<Index> = listOf(
        Index()
            .on("${ReversedEthereumLogRecord::data.name}.${RoyaltiesEvent::token.name}", Sort.Direction.ASC)
            .on(ReversedEthereumLogRecord::blockNumber.name, Sort.Direction.ASC)
            .on(ReversedEthereumLogRecord::logIndex.name, Sort.Direction.ASC)
            .on(ReversedEthereumLogRecord::minorLogIndex.name, Sort.Direction.ASC)
            .background()
    )
}
