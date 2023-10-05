package com.rarible.protocol.order.core.repository.exchange

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.ActivitySort
import com.rarible.protocol.order.core.model.AggregatedData
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.LogEventShort
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepositoryIndexes.ALL_INDEXES
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepositoryIndexes.ITEM_BID_DEFINITION
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepositoryIndexes.ITEM_SELL_DEFINITION
import com.rarible.protocol.order.core.repository.exchange.misc.aggregateWithHint
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.types.ObjectId
import org.jetbrains.annotations.TestOnly
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address
import java.util.Date

@Component
class ExchangeHistoryRepository(
    private val template: ReactiveMongoTemplate
) {

    suspend fun dropIndexes() {
        dropIndexes(
            "data.date_1_id_1",
            "data.make.type.nft_1_data.date_-1_id_-1",
            "data.make.type.nft_1_data.make.type.token_1_data.date_-1_id_-1",
            "data.make.type.nft_1_data.maker_1_data.date_-1_id_-1",
            "data.make.type.token_1_data.make.type.tokenId_1_data.date_-1_id_-1",
            "data.make.type.token_1_data.make.type.tokenId_1_data.date_-1_id_-1",
            "data.make.type.nft_1_data.date_1__id_-1",
            "right_data.make.type.nft_1_data.date_1__id_1"
        )
    }

    @Suppress("SameParameterValue")
    private suspend fun dropIndexes(vararg names: String) {
        val existing = template.indexOps(COLLECTION).indexInfo.map { it.name }.collectList().awaitFirst()
        for (name in names) {
            if (existing.contains(name)) {
                logger.info("dropping index $name")
                template.indexOps(COLLECTION).dropIndex(name).awaitFirstOrNull()
            } else {
                logger.info("skipping drop index $name")
            }
        }
    }

    suspend fun createIndexes() {
        ALL_INDEXES.forEach { index ->
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

    @TestOnly // this query may be slow, use in tests only
    fun findByItemType(type: ItemType): Flux<ReversedEthereumLogRecord> {
        val query = Query(ReversedEthereumLogRecord::topic inValues type.topic)
        return template.find(query, COLLECTION)
    }

    fun findSellEventsByItem(token: Address, tokenId: EthUInt256): Flux<ReversedEthereumLogRecord> {
        val tokenKey = ReversedEthereumLogRecord::data / OrderExchangeHistory::make / Asset::type / NftAssetType::token
        val tokenIdKey = ReversedEthereumLogRecord::data / OrderExchangeHistory::make / Asset::type / NftAssetType::tokenId
        val criteria = tokenKey.isEqualTo(token)
            .and(tokenIdKey).isEqualTo(tokenId)
            .and(ReversedEthereumLogRecord::status).isEqualTo(EthereumBlockStatus.CONFIRMED)
        val query = Query
            .query(criteria)
            .with(
                Sort.by(
                    Sort.Order.asc("${ReversedEthereumLogRecord::data.name}.${OrderExchangeHistory::date.name}"),
                    Sort.Order.asc(OrderVersion::id.name)
                )
            )
            .withHint(ITEM_SELL_DEFINITION.indexKeys)
        return template.find(query, COLLECTION)
    }

    fun findBidEventsByItem(token: Address, tokenId: EthUInt256): Flux<ReversedEthereumLogRecord> {
        val tokenKey = ReversedEthereumLogRecord::data / OrderExchangeHistory::take / Asset::type / NftAssetType::token
        val tokenIdKey = ReversedEthereumLogRecord::data / OrderExchangeHistory::take / Asset::type / NftAssetType::tokenId
        val criteria = tokenKey.isEqualTo(token)
            .and(tokenIdKey).isEqualTo(tokenId)
            .and(ReversedEthereumLogRecord::status).isEqualTo(EthereumBlockStatus.CONFIRMED)
        val query = Query
            .query(criteria)
            .with(
                Sort.by(
                    Sort.Order.asc("${ReversedEthereumLogRecord::data.name}.${OrderExchangeHistory::date.name}"),
                    Sort.Order.asc(OrderVersion::id.name)
                )
            )
            .withHint(ITEM_BID_DEFINITION.indexKeys)
        return template.find(query, COLLECTION)
    }

    fun findReversedEthereumLogRecords(hash: Word?, from: Word?, platforms: List<HistorySource>? = null): Flux<ReversedEthereumLogRecord> {
        var criteria = when {
            hash != null -> ReversedEthereumLogRecord::data / OrderExchangeHistory::hash isEqualTo hash
            from != null -> ReversedEthereumLogRecord::data / OrderExchangeHistory::hash gt from
            else -> Criteria()
        }
        if (platforms?.isNotEmpty() == true) {
            criteria = criteria.and(ReversedEthereumLogRecord::data / OrderExchangeHistory::source).inValues(platforms)
        }
        val query = Query(criteria).with(LOG_SORT_ASC)
        return template.find(query, COLLECTION)
    }

    fun findIgnoredEvents(from: String?, historySource: HistorySource): Flow<ReversedEthereumLogRecord> {
        val criteria = when {
            from != null -> Criteria.where("_id").gt(ObjectId(from))
            else -> Criteria()
        }
        criteria
            .and(ReversedEthereumLogRecord::data / OrderExchangeHistory::source).isEqualTo(historySource)
            .and(ReversedEthereumLogRecord::data / OrderSideMatch::ignoredEvent).isEqualTo(true)

        val query = Query(criteria).with(LOG_ID_SORT_ASC)
        return template.find<ReversedEthereumLogRecord>(query, COLLECTION).asFlow()
    }

    fun findAll(): Flux<ReversedEthereumLogRecord> {
        return template.findAll(COLLECTION)
    }

    fun searchActivity(filter: ActivityExchangeHistoryFilter): Flux<ReversedEthereumLogRecord> {
        val hint = filter.hint
        val criteria = filter.getCriteria()
            .and(ReversedEthereumLogRecord::status).isEqualTo(filter.status)

        val query = Query(criteria)

        if (hint != null) {
            query.withHint(hint)
        }
        return template.find(query.with(filter.sort.toMongo()), ReversedEthereumLogRecord::class.java, COLLECTION)
    }

    fun searchShortActivity(filter: ActivityExchangeHistoryFilter): Flux<ObjectId> {
        val hint = filter.hint
        val criteria = filter.getCriteria().and(ReversedEthereumLogRecord::status).isEqualTo(filter.status)
        val query = Query(criteria)

        if (hint != null) {
            query.withHint(hint)
        }
        return template.find(query.with(filter.sort.toMongo()), LogEventShort::class.java, COLLECTION).map { it.id }
    }

    fun findByIds(ids: List<ObjectId>): Flux<ReversedEthereumLogRecord> {
        val query = Query(ReversedEthereumLogRecord::id inValues ids)
        return template.find(query, ReversedEthereumLogRecord::class.java, COLLECTION)
    }

    // TODO remove later
    fun <T> aggregate(aggregation: Aggregation, collectionName: String, outputType: Class<T>): Flux<T> {
        return template.aggregateWithHint(aggregation, collectionName, outputType, null)
    }

    private fun ActivitySort.toMongo() =
        when (this) {
            ActivitySort.LATEST_FIRST -> Sort.by(
                Sort.Order.desc("${ReversedEthereumLogRecord::data.name}.${OrderExchangeHistory::date.name}"),
                Sort.Order.desc(OrderVersion::id.name)
            )
            ActivitySort.EARLIEST_FIRST -> Sort.by(
                Sort.Order.asc("${ReversedEthereumLogRecord::data.name}.${OrderExchangeHistory::date.name}"),
                Sort.Order.asc(OrderVersion::id.name)
            )
            ActivitySort.SYNC_LATEST_FIRST -> Sort.by(
                Sort.Order.desc(ReversedEthereumLogRecord::updatedAt.name),
                Sort.Order.desc(ReversedEthereumLogRecord::id.name)
            )
            ActivitySort.SYNC_EARLIEST_FIRST -> Sort.by(
                Sort.Order.asc(ReversedEthereumLogRecord::updatedAt.name),
                Sort.Order.asc(ReversedEthereumLogRecord::id.name)
            )
            ActivitySort.BY_ID -> Sort.by(
                Sort.Order.asc(ReversedEthereumLogRecord::id.name)
            )
        }

    fun getMakerNftSellAggregation(startDate: Date, endDate: Date, source: HistorySource?): Flux<AggregatedData> {
        return getNftPurchaseAggregation(
            "${ReversedEthereumLogRecord::data.name}.${OrderExchangeHistory::maker.name}",
            source,
            startDate,
            endDate
        )
    }

    fun getTakerNftBuyAggregation(startDate: Date, endDate: Date, source: HistorySource?): Flux<AggregatedData> {
        return getNftPurchaseAggregation(
            "${ReversedEthereumLogRecord::data.name}.${OrderSideMatch::taker.name}",
            source,
            startDate,
            endDate
        )
    }

    fun getTokenPurchaseAggregation(startDate: Date, endDate: Date, source: HistorySource?): Flux<AggregatedData> {
        return getNftPurchaseAggregation(
            "${ReversedEthereumLogRecord::data.name}.${OrderSideMatch::make.name}.${Asset::type.name}.${NftAssetType::token.name}",
            source,
            startDate,
            endDate
        )
    }

    private fun getNftPurchaseAggregation(
        groupByFiled: String,
        source: HistorySource?,
        startDate: Date,
        endDate: Date
    ): Flux<AggregatedData> {
        val match = Aggregation.match(
            (ReversedEthereumLogRecord::data / OrderExchangeHistory::make / Asset::type / AssetType::nft isEqualTo true)
                .and(ReversedEthereumLogRecord::data / OrderExchangeHistory::type).isEqualTo(ItemType.ORDER_SIDE_MATCH)
                .and(ReversedEthereumLogRecord::data / OrderExchangeHistory::date).gt(startDate).lt(endDate)
                .and(ReversedEthereumLogRecord::status).inValues(EthereumBlockStatus.PENDING, EthereumBlockStatus.CONFIRMED)
                .run { source?.let { and(ReversedEthereumLogRecord::data / OrderExchangeHistory::source).isEqualTo(it) } ?: this }
        )
        val group = Aggregation
            .group(groupByFiled)
            .sum("${ReversedEthereumLogRecord::data.name}.${OrderSideMatch::takeUsd.name}").`as`(AggregatedData::sum.name)
            .count().`as`(AggregatedData::count.name)

        val sort = Aggregation.sort(Sort.by(Sort.Direction.DESC, AggregatedData::sum.name))
        val aggregation = Aggregation.newAggregation(match, group, sort)

        return template.aggregateWithHint(
            aggregation,
            COLLECTION,
            AggregatedData::class.java,
            ExchangeHistoryRepositoryIndexes.AGGREGATION_DEFINITION.indexKeys
        )
    }

    companion object {
        const val COLLECTION = "exchange_history"

        val LOG_SORT_ASC: Sort = Sort
            .by(
                "${ReversedEthereumLogRecord::data.name}.${OrderExchangeHistory::hash.name}",
                ReversedEthereumLogRecord::blockNumber.name,
                ReversedEthereumLogRecord::logIndex.name,
                ReversedEthereumLogRecord::minorLogIndex.name
            )

        val LOG_ID_SORT_ASC: Sort = Sort.by("_id")

        val logger: Logger = LoggerFactory.getLogger(ExchangeHistoryRepository::class.java)
    }
}
