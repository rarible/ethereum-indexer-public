package com.rarible.protocol.nft.core.repository.history

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.misc.div
import com.rarible.protocol.nft.core.model.HistoryLog
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepositoryIndexes.ALL_INDEXES
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.*
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address

@Component
@CaptureSpan(type = SpanType.DB)
class NftItemHistoryRepository(
    private val mongo: ReactiveMongoOperations
) {
    suspend fun dropIndexes() {
        dropIndexes(
            "data.from_1_data.date_-1_id_-1",
            "data.owner_1_data.date_-1_id_-1",
            "data.token_1_data.date_-1_id_-1",
            "data.type_1_data.date_-1_id_-1",
            "data.type_1_status_1_data.from_1_data.date_-1_id_-1",
            "data.type_1_status_1_data.owner_1_data.date_-1_id_-1",
            "data.type_1_status_1_data.token_1_data.date_-1_id_-1"
        )
    }

    @Suppress("SameParameterValue")
    private suspend fun dropIndexes(vararg names: String) {
        val existing = mongo.indexOps(COLLECTION).indexInfo.map { it.name }.collectList().awaitFirst()
        for (name in names) {
            if (existing.contains(name)) {
                logger.info("dropping index $name")
                mongo.indexOps(COLLECTION).dropIndex(name).awaitFirstOrNull()
            } else {
                logger.info("skipping drop index $name")
            }
        }
    }

    suspend fun createIndexes() {
        ALL_INDEXES.forEach { index ->
            mongo.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    fun save(event: LogEvent): Mono<LogEvent> {
        return mongo.save(event, COLLECTION)
    }

    fun findAllItemsHistory(): Flux<HistoryLog> {
        return findItemsHistory(null, null)
    }

    fun findItemsHistory(
        token: Address? = null,
        tokenId: EthUInt256? = null,
        from: ItemId? = null,
        to: ItemId? = null,
        statuses: List<LogEventStatus>? = null
    ): Flux<HistoryLog> {
        val criteria = tokenCriteria(token, tokenId, from, to)
        return mongo
            .find(Query(criteria).with(LOG_SORT_ASC), LogEvent::class.java, COLLECTION)
            .map { HistoryLog(it.data as ItemHistory, it) }
    }

    private fun tokenCriteria(
        token: Address?,
        tokenId: EthUInt256?,
        from: ItemId? = null,
        to: ItemId? = null
    ): Criteria {
        return when {
            token != null && tokenId != null ->
                Criteria().andOperator(
                    LogEvent::data / ItemHistory::token isEqualTo token,
                    LogEvent::data / ItemHistory::tokenId isEqualTo tokenId
                )
            token != null && from != null ->
                Criteria().andOperator(
                    LogEvent::data / ItemHistory::token isEqualTo token,
                    LogEvent::data / ItemHistory::tokenId gt from.tokenId
                )
            token != null ->
                LogEvent::data / ItemHistory::token isEqualTo token
            from != null && to == null ->
                Criteria().orOperator(
                    Criteria().andOperator(
                        LogEvent::data / ItemHistory::token isEqualTo from.token,
                        LogEvent::data / ItemHistory::tokenId gt from.tokenId
                    ),
                    LogEvent::data / ItemHistory::token gt from.token
                )
            from != null && to != null ->
                Criteria().andOperator(
                    Criteria().orOperator(
                        Criteria().andOperator(
                            LogEvent::data / ItemHistory::token isEqualTo from.token,
                            LogEvent::data / ItemHistory::tokenId gt from.tokenId
                        ),
                        LogEvent::data / ItemHistory::token gt from.token
                    ),
                    LogEvent::data / ItemHistory::token lt to.token
                )
            else ->
                Criteria()
        }
    }

    suspend fun search(query: Query): List<LogEvent> {
        return mongo.find(query, LogEvent::class.java, COLLECTION)
            .collectList()
            .awaitFirst()
    }

    fun searchActivity(filter: ActivityItemHistoryFilter): Flux<LogEvent> {
        val hint = filter.hint
        val sort = filter.sort
        val criteria = filter.getCriteria()

        val query = Query(criteria)

        if (hint != null) {
            query.withHint(hint)
        }
        query.with(sort.sort)
        return mongo.find(query, LogEvent::class.java, COLLECTION)
    }

    companion object {
        const val COLLECTION = "nft_item_history"

        val logger: Logger = LoggerFactory.getLogger(NftItemHistoryRepository::class.java)

        val LOG_SORT_ASC: Sort = Sort.by(
            "${LogEvent::data.name}.${ItemTransfer::token.name}",
            "${LogEvent::data.name}.${ItemTransfer::tokenId.name}",
            LogEvent::blockNumber.name,
            LogEvent::logIndex.name
        )
    }
}
