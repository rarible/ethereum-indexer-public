package com.rarible.protocol.order.core.repository.auction

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.reduce.repository.ReduceEventRepository
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.AuctionHistory
import com.rarible.protocol.order.core.model.AuctionHistoryType
import com.rarible.protocol.order.core.model.AuctionReduceEvent
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.OnChainAuction
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
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CaptureSpan(type = SpanType.DB)
@Component
class AuctionHistoryRepository(
    private val template: ReactiveMongoTemplate
) : ReduceEventRepository<AuctionReduceEvent, Long, Word> {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun save(reversedEthereumLogRecord: ReversedEthereumLogRecord): Mono<ReversedEthereumLogRecord> {
        return template.save(reversedEthereumLogRecord.withUpdatedAt(), COLLECTION)
    }

    fun find(query: Query): Flow<ReversedEthereumLogRecord> {
        return template.find(query, ReversedEthereumLogRecord::class.java, COLLECTION).asFlow()
    }

    fun findById(id: ObjectId): Mono<ReversedEthereumLogRecord> {
        return template.findById(id, COLLECTION)
    }

    fun findByType(type: AuctionHistoryType): Flux<ReversedEthereumLogRecord> {
        val query = Query(ReversedEthereumLogRecord::topic inValues type.topic)
        return template.find(query, COLLECTION)
    }

    fun searchActivity(filter: ActivityAuctionHistoryFilter, size: Int): Flux<ReversedEthereumLogRecord> {
        val hint = filter.hint
        val criteria = filter.getCriteria().and(ReversedEthereumLogRecord::status).isEqualTo(EthereumBlockStatus.CONFIRMED)

        val query = Query(criteria).limit(size)

        if (hint != null) {
            query.withHint(hint)
        }
        return template.find(query.with(filter.sort), ReversedEthereumLogRecord::class.java, COLLECTION)
    }

    override fun getEvents(key: Word?, after: Long?): Flow<AuctionReduceEvent> {
        val criteria = Criteria()
            .run {
                key?.let { and(ReversedEthereumLogRecord::data / AuctionHistory::hash).isEqualTo(key) } ?: this
            }
            .run {
                after?.let { and(ReversedEthereumLogRecord::blockNumber).gt(it) } ?: this
            }

        val query = Query(criteria).with(LOG_SORT_ASC)
        return template
            .find(query, ReversedEthereumLogRecord::class.java, COLLECTION)
            .map { reversedEthereumLogRecord -> AuctionReduceEvent(reversedEthereumLogRecord) }
            .asFlow()
    }

    fun findAll(): Flux<ReversedEthereumLogRecord> {
        return template.findAll(COLLECTION)
    }

    suspend fun createIndexes() {
        AuctionHistoryIndexes.ALL_INDEXES.forEach { index ->
            template.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    fun findByIds(ids: List<String>): Flux<ReversedEthereumLogRecord> {
        val query = Query(
            ReversedEthereumLogRecord::id inValues ids.map { ObjectId(it) }
        )
        return template.find(query, ReversedEthereumLogRecord::class.java, COLLECTION)
    }

    private object AuctionHistoryIndexes {
        val BY_TYPE_TOKEN_ID_DEFINITION: Index = Index()
            .on("${ReversedEthereumLogRecord::data.name}.${AuctionHistory::type.name}", Sort.Direction.ASC)
            .on("${ReversedEthereumLogRecord::data.name}.${Auction::sell.name}.${Asset::type.name}.${NftAssetType::token::name}", Sort.Direction.ASC)
            .on("${ReversedEthereumLogRecord::data.name}.${Auction::sell.name}.${Asset::type.name}.${NftAssetType::tokenId::name}", Sort.Direction.ASC)
            .background()

        val BY_TYPE_SELLER_DEFINITION: Index = Index()
            .on("${ReversedEthereumLogRecord::data.name}.${AuctionHistory::type.name}", Sort.Direction.ASC)
            .on("${ReversedEthereumLogRecord::data.name}.${OnChainAuction::seller.name}", Sort.Direction.ASC)
            .background()

        val BY_TYPE_BUYER_DEFINITION: Index = Index()
            .on("${ReversedEthereumLogRecord::data.name}.${AuctionHistory::type.name}", Sort.Direction.ASC)
            .on("${ReversedEthereumLogRecord::data.name}.${OnChainAuction::buyer.name}", Sort.Direction.ASC)
            .background()

        val BY_UPDATED_AT_FIELD: Index = Index()
            .on(ReversedEthereumLogRecord::updatedAt.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        val ALL_INDEXES = listOf(
            BY_TYPE_TOKEN_ID_DEFINITION,
            BY_TYPE_SELLER_DEFINITION,
            BY_TYPE_BUYER_DEFINITION,
            BY_UPDATED_AT_FIELD
        )
    }

    companion object {
        const val COLLECTION = "auction_history"

        val LOG_SORT_ASC: Sort = Sort
            .by(
                "${ReversedEthereumLogRecord::data.name}.${AuctionHistory::hash.name}",
                ReversedEthereumLogRecord::blockNumber.name,
                ReversedEthereumLogRecord::logIndex.name,
                ReversedEthereumLogRecord::minorLogIndex.name
            )
    }
}
