package com.rarible.protocol.erc20.core.repository

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.mongo.util.div
import com.rarible.protocol.erc20.core.converters.Erc20HistoryLogConverter
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20HistoryLog
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.types.ObjectId
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address

@Component
class Erc20TransferHistoryRepository(
    private val template: ReactiveMongoTemplate
) {

    fun save(event: ReversedEthereumLogRecord): Mono<ReversedEthereumLogRecord> {
        return template.save(event, COLLECTION)
    }

    fun findBalanceLogEvents(
        balanceId: BalanceId?,
        fromBlockNumber: Long?
    ): Flux<ReversedEthereumLogRecord> {
        val criteria = Criteria()
            .run {
                balanceId?.let {
                    and(ReversedEthereumLogRecord::data / Erc20TokenHistory::token).isEqualTo(it.token)
                        .and(ReversedEthereumLogRecord::data / Erc20TokenHistory::owner).isEqualTo(it.owner)
                } ?: this
            }
            .run {
                fromBlockNumber?.let {
                    and(ReversedEthereumLogRecord::blockNumber).gt(it)
                } ?: this
            }.confirmed()

        val query = Query(criteria).with(LOG_SORT_ASC).cursorBatchSize(BATCH_SIZE)
        return template.find(query, ReversedEthereumLogRecord::class.java, COLLECTION)
    }

    fun findBalanceLogEventsForToken(token: Address, afterOwner: Address?): Flux<ReversedEthereumLogRecord> {
        val criteria = where(ReversedEthereumLogRecord::data / Erc20TokenHistory::token).isEqualTo(token).run {
            afterOwner?.let {
                and(ReversedEthereumLogRecord::data / Erc20TokenHistory::owner).gt(it)
            } ?: this
        }.confirmed()
        val query = Query(criteria).with(LOG_SORT_ASC).cursorBatchSize(BATCH_SIZE)
        return template.find(query, ReversedEthereumLogRecord::class.java, COLLECTION)
    }

    fun findOwnerLogEvents(
        token: Address? = null,
        owner: Address? = null,
        from: BalanceId? = null
    ): Flux<Erc20HistoryLog> {
        val criteria = when {
            token != null && owner != null -> {
                Criteria.where(DATA_TOKEN).`is`(token).and(DATA_OWNER).`is`(owner)
            }
            token != null && from != null -> {
                Criteria.where(DATA_TOKEN).`is`(token).and(DATA_OWNER).gt(from.owner)
            }
            token != null -> {
                Criteria.where(DATA_TOKEN).`is`(token)
            }
            from != null -> {
                Criteria().orOperator(
                    Criteria.where(DATA_TOKEN).`is`(from.token).and(DATA_OWNER).gt(from.owner),
                    Criteria.where(DATA_TOKEN).gt(from.token)
                )
            }
            else -> {
                Criteria()
            }
        }.confirmed()

        val query = Query(criteria).with(LOG_SORT_ASC).cursorBatchSize(BATCH_SIZE)
        return template
            .find(query, ReversedEthereumLogRecord::class.java, COLLECTION)
            .mapNotNull {
                Erc20HistoryLogConverter.convert(it)
            }
    }

    fun Criteria.confirmed() = this.and(ReversedEthereumLogRecord::status).isEqualTo(EthereumBlockStatus.CONFIRMED)

    fun findAll(fromIdExcluded: String?): Flow<ReversedEthereumLogRecord> =
        template.find(
            Query(Criteria().apply {
                if (fromIdExcluded != null) {
                    and(ReversedEthereumLogRecord::id).gt(ObjectId(fromIdExcluded))
                }
            }.confirmed()).with(Sort.by(ReversedEthereumLogRecord::id.name)),
            ReversedEthereumLogRecord::class.java,
            COLLECTION
        ).asFlow()

    suspend fun findPossibleDuplicates(logEvent: ReversedEthereumLogRecord): List<ReversedEthereumLogRecord> {
        val historyLog = Erc20HistoryLogConverter.convert(logEvent) ?: return emptyList()
        return template.find(
            Query(
                Criteria()
                    .and(ReversedEthereumLogRecord::transactionHash).isEqualTo(logEvent.transactionHash)
                    .and(ReversedEthereumLogRecord::blockHash).isEqualTo(logEvent.blockHash)
                    .and(ReversedEthereumLogRecord::logIndex).isEqualTo(logEvent.logIndex)
                    .and(ReversedEthereumLogRecord::data / Erc20TokenHistory::owner).isEqualTo(historyLog.history.owner)
                    .and(ReversedEthereumLogRecord::data / Erc20TokenHistory::token).isEqualTo(historyLog.history.token)
                    .and(ReversedEthereumLogRecord::id).ne(logEvent.id)
            ).with(Sort.by(ReversedEthereumLogRecord::id.name)), ReversedEthereumLogRecord::class.java, COLLECTION
        ).asFlow().toList()
    }

    suspend fun removeAll(logs: List<ReversedEthereumLogRecord>) {
        logs.forEach {
            template.remove(it).awaitFirstOrNull()
        }
    }

    suspend fun deleteByOwner(owner: Address): Long {
        val criteria = Criteria(DATA_OWNER).isEqualTo(owner)
        return template.remove(Query.query(criteria), COLLECTION).awaitFirst().deletedCount
    }

    companion object {

        const val COLLECTION = "erc20_history"

        // Default batch = 32, it's better to use bigger size to get all records in single query from cursor
        const val BATCH_SIZE = 100_000

        val DATA_TOKEN = "${ReversedEthereumLogRecord::data.name}.${Erc20TokenHistory::token.name}"
        val DATA_OWNER = "${ReversedEthereumLogRecord::data.name}.${Erc20TokenHistory::owner.name}"

        val LOG_SORT_ASC: Sort = Sort
            .by(
                DATA_TOKEN,
                DATA_OWNER,
                ReversedEthereumLogRecord::blockNumber.name,
                ReversedEthereumLogRecord::logIndex.name,
                ReversedEthereumLogRecord::minorLogIndex.name
            )
    }
}
