package com.rarible.protocol.order.core.repository.pool

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolDataUpdate
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.model.PoolHistoryType
import com.rarible.protocol.order.core.model.PoolNftChange
import com.rarible.protocol.order.core.repository.pool.PoolHistoryRepository.Indexes.HASH_DEFINITION
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address

@Component
class PoolHistoryRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun createIndexes() {
        Indexes.ALL_INDEXES.forEach { index ->
            template.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    fun save(reversedEthereumLogRecord: ReversedEthereumLogRecord): Mono<ReversedEthereumLogRecord> {
        return template.save(reversedEthereumLogRecord, COLLECTION)
    }

    fun find(query: Query): Flow<ReversedEthereumLogRecord> {
        return template.find(query, ReversedEthereumLogRecord::class.java, COLLECTION).asFlow()
    }

    fun findById(id: ObjectId): Mono<ReversedEthereumLogRecord> {
        return template.findById(id, COLLECTION)
    }

    fun findByIds(ids: List<ObjectId>): Flux<ReversedEthereumLogRecord> {
        val query = Query(ReversedEthereumLogRecord::id inValues ids)
        return template.find(query, ReversedEthereumLogRecord::class.java, COLLECTION)
    }

    fun findDistinctHashes(from: Word? = null): Flow<Word> {
        val hashFiled = "${ReversedEthereumLogRecord::data.name}.${PoolHistory::hash.name}"
        val criteria = when {
            from != null -> Criteria(hashFiled).gt(from)
            else -> Criteria()
        }
        val query = Query(criteria).withHint(Indexes.HASH_DEFINITION.indexKeys)
        query.fields().include(hashFiled)
        return template.findDistinct(query, hashFiled, COLLECTION, Word::class.java).asFlow()
    }

    fun findReversedEthereumLogRecords(hash: Word?, from: Word?, platforms: List<HistorySource>? = null): Flux<ReversedEthereumLogRecord> {
        var criteria = when {
            hash != null -> ReversedEthereumLogRecord::data / PoolHistory::hash isEqualTo hash
            from != null -> ReversedEthereumLogRecord::data / PoolHistory::hash gt from
            else -> Criteria()
        }
        if (platforms?.isNotEmpty() == true) {
            criteria = criteria.and(ReversedEthereumLogRecord::data / PoolHistory::source).inValues(platforms)
        }
        val query = Query(criteria).with(LOG_SORT_ASC)
        return template.find(query, COLLECTION)
    }

    suspend fun getPoolCreateEvent(hash: Word): ReversedEthereumLogRecord? {
        val criteria = Criteria().andOperator(
            ReversedEthereumLogRecord::data / PoolHistory::hash isEqualTo hash,
            ReversedEthereumLogRecord::data / PoolHistory::type isEqualTo PoolHistoryType.POOL_CREAT,
            ReversedEthereumLogRecord::status isEqualTo EthereumBlockStatus.CONFIRMED,
        )
        val query = Query(criteria)
            .with(POOL_CHANGE_SORT_ASC)
            .withHint(HASH_DEFINITION.indexKeys)
            .limit(1)

        return template.findOne(query, ReversedEthereumLogRecord::class.java, COLLECTION).awaitFirstOrNull()
    }

    suspend fun getLatestPoolNftChange(collection: Address, tokenId: EthUInt256): List<ReversedEthereumLogRecord> {
        val criteria = Criteria().andOperator(
            ReversedEthereumLogRecord::data / PoolNftChange::collection isEqualTo collection,
            ReversedEthereumLogRecord::data / PoolNftChange::tokenIds isEqualTo tokenId,
            ReversedEthereumLogRecord::status isEqualTo EthereumBlockStatus.CONFIRMED,
        )
        val query = Query(criteria)
            .with(POOL_CHANGE_SORT_DESC)
            // TODO ideally we should get rid of direct hint usages
            // .withHint(Indexes.NFT_CHANGES_POOL_ITEM_IDS_DEFINITION.indexKeys)
            .limit(1)

        return template.find<ReversedEthereumLogRecord>(query, COLLECTION).collectList().awaitFirst()
    }

    suspend fun getLatestPoolEvent(
        hash: Word,
        type: PoolHistoryType,
        blockNumber: Long,
        logIndex: Int
    ): ReversedEthereumLogRecord? {
        val criteria = Criteria().andOperator(
            ReversedEthereumLogRecord::data / PoolDataUpdate::hash isEqualTo hash,
            ReversedEthereumLogRecord::data / PoolDataUpdate::type isEqualTo type,
            ReversedEthereumLogRecord::status isEqualTo EthereumBlockStatus.CONFIRMED,
            Criteria().orOperator(
                Criteria().andOperator(
                    ReversedEthereumLogRecord::blockNumber isEqualTo blockNumber,
                    ReversedEthereumLogRecord::logIndex lt logIndex
                ),
                ReversedEthereumLogRecord::blockNumber lt blockNumber,
            )
        )
        val query = Query(criteria)
            .with(POOL_CHANGE_SORT_DESC)
            .withHint(Indexes.HASH_DEFINITION.indexKeys)
            .limit(1)

        return template.findOne<ReversedEthereumLogRecord>(query, COLLECTION).awaitFirstOrNull()
    }

    fun findAll(): Flux<ReversedEthereumLogRecord> {
        return template.findAll(COLLECTION)
    }

    private object Indexes {
        val HASH_DEFINITION: Index = Index()
            .on("${ReversedEthereumLogRecord::data.name}.${PoolHistory::hash.name}", Sort.Direction.ASC)
            .on(ReversedEthereumLogRecord::blockNumber.name, Sort.Direction.ASC)
            .on(ReversedEthereumLogRecord::logIndex.name, Sort.Direction.ASC)
            .on(ReversedEthereumLogRecord::minorLogIndex.name, Sort.Direction.ASC)
            .background()

        val BY_UPDATED_AT_FIELD: Index = Index()
            .on(ReversedEthereumLogRecord::updatedAt.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        val NFT_CHANGES_POOL_ITEM_IDS_DEFINITION: Index = Index()
            .on("${ReversedEthereumLogRecord::data.name}.${PoolNftChange::collection.name}", Sort.Direction.ASC)
            .on("${ReversedEthereumLogRecord::data.name}.${PoolNftChange::tokenIds.name}", Sort.Direction.ASC)
            .on(ReversedEthereumLogRecord::blockNumber.name, Sort.Direction.ASC)
            .on(ReversedEthereumLogRecord::logIndex.name, Sort.Direction.ASC)
            .on(ReversedEthereumLogRecord::minorLogIndex.name, Sort.Direction.ASC)
            .background()

        val ALL_INDEXES = listOf(
            HASH_DEFINITION,
            BY_UPDATED_AT_FIELD,
            NFT_CHANGES_POOL_ITEM_IDS_DEFINITION
        )
    }

    companion object {
        const val COLLECTION = "pool_history"

        val POOL_CHANGE_SORT_DESC: Sort = Sort
            .by(
                Sort.Order(Sort.Direction.DESC, ReversedEthereumLogRecord::blockNumber.name),
                Sort.Order(Sort.Direction.DESC, ReversedEthereumLogRecord::logIndex.name),
                Sort.Order(Sort.Direction.DESC, ReversedEthereumLogRecord::minorLogIndex.name),
            )

        val POOL_CHANGE_SORT_ASC: Sort = Sort
            .by(
                Sort.Order(Sort.Direction.ASC, ReversedEthereumLogRecord::blockNumber.name),
                Sort.Order(Sort.Direction.ASC, ReversedEthereumLogRecord::logIndex.name),
                Sort.Order(Sort.Direction.ASC, ReversedEthereumLogRecord::minorLogIndex.name),
            )

        val LOG_SORT_ASC: Sort = Sort
            .by(
                "${ReversedEthereumLogRecord::data.name}.${PoolHistory::hash.name}",
                ReversedEthereumLogRecord::blockNumber.name,
                ReversedEthereumLogRecord::logIndex.name,
                ReversedEthereumLogRecord::minorLogIndex.name
            )

        val logger: Logger = LoggerFactory.getLogger(PoolHistoryRepository::class.java)
    }
}
