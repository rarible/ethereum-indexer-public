package com.rarible.protocol.order.core.repository.exchange

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.ActivitySort
import com.rarible.protocol.order.core.model.AggregatedData
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepositoryIndexes.ALL_INDEXES
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepositoryIndexes.ITEM_BID_DEFINITION
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepositoryIndexes.ITEM_SELL_DEFINITION
import com.rarible.protocol.order.core.repository.exchange.misc.aggregateWithHint
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.jetbrains.annotations.TestOnly
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findAll
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
import java.util.*

@CaptureSpan(type = SpanType.DB)
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
            "data.make.type.nft_1_data.date_1__id_-1"
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

    fun save(logEvent: LogEvent): Mono<LogEvent> {
        return template.save(logEvent, COLLECTION)
    }

    @TestOnly // this query may be slow, use in tests only
    fun findByItemType(type: ItemType): Flux<LogEvent> {
        val query = Query(LogEvent::topic inValues type.topic)
        return template.find(query, COLLECTION)
    }

    fun findSellEventsByItem(token: Address, tokenId: EthUInt256): Flux<LogEvent> {
        val tokenKey = LogEvent::data / OrderExchangeHistory::make / Asset::type / NftAssetType::token
        val tokenIdKey = LogEvent::data / OrderExchangeHistory::make / Asset::type / NftAssetType::tokenId
        val criteria = tokenKey.isEqualTo(token)
            .and(tokenIdKey).isEqualTo(tokenId)
            .and(LogEvent::status).isEqualTo(LogEventStatus.CONFIRMED)
        val query = Query
            .query(criteria)
            .with(
                Sort.by(
                    Sort.Order.asc("${LogEvent::data.name}.${OrderExchangeHistory::date.name}"),
                    Sort.Order.asc(OrderVersion::id.name)
                )
            )
            .withHint(ITEM_SELL_DEFINITION.indexKeys)
        return template.find(query, COLLECTION)
    }

    fun findBidEventsByItem(token: Address, tokenId: EthUInt256): Flux<LogEvent> {
        val tokenKey = LogEvent::data / OrderExchangeHistory::take / Asset::type / NftAssetType::token
        val tokenIdKey = LogEvent::data / OrderExchangeHistory::take / Asset::type / NftAssetType::tokenId
        val criteria = tokenKey.isEqualTo(token)
            .and(tokenIdKey).isEqualTo(tokenId)
            .and(LogEvent::status).isEqualTo(LogEventStatus.CONFIRMED)
        val query = Query
            .query(criteria)
            .with(
                Sort.by(
                    Sort.Order.asc("${LogEvent::data.name}.${OrderExchangeHistory::date.name}"),
                    Sort.Order.asc(OrderVersion::id.name)
                )
            )
            .withHint(ITEM_BID_DEFINITION.indexKeys)
        return template.find(query, COLLECTION)
    }

    fun findLogEvents(hash: Word?, from: Word?): Flux<LogEvent> {
        val criteria = when {
            hash != null -> LogEvent::data / OrderExchangeHistory::hash isEqualTo hash
            from != null -> LogEvent::data / OrderExchangeHistory::hash gt from
            else -> Criteria()
        }

        val query = Query(criteria).with(LOG_SORT_ASC)
        return template.find(query, COLLECTION)
    }

    fun findAll(): Flux<LogEvent> {
        return template.findAll(COLLECTION)
    }

    fun searchActivity(filter: ActivityExchangeHistoryFilter): Flux<LogEvent> {
        val hint = filter.hint
        val criteria = filter.getCriteria()
            .and(LogEvent::status).isEqualTo(LogEventStatus.CONFIRMED)

        val query = Query(criteria)

        if (hint != null) {
            query.withHint(hint)
        }
        return template.find(query.with(filter.sort.toMongo()), LogEvent::class.java, COLLECTION)
    }

    private fun ActivitySort.toMongo() =
        when(this) {
            ActivitySort.LATEST_FIRST -> Sort.by(
                Sort.Order.desc("${LogEvent::data.name}.${OrderExchangeHistory::date.name}"),
                Sort.Order.desc(OrderVersion::id.name)
            )
            ActivitySort.EARLIEST_FIRST -> Sort.by(
                Sort.Order.asc("${LogEvent::data.name}.${OrderExchangeHistory::date.name}"),
                Sort.Order.asc(OrderVersion::id.name)
            )
        }

    fun getMakerNftSellAggregation(startDate: Date, endDate: Date, source: HistorySource?): Flux<AggregatedData> {
        return getNftPurchaseAggregation(
            "${LogEvent::data.name}.${OrderExchangeHistory::maker.name}",
            source,
            startDate,
            endDate
        )
    }

    fun getTakerNftBuyAggregation(startDate: Date, endDate: Date, source: HistorySource?): Flux<AggregatedData> {
        return getNftPurchaseAggregation(
            "${LogEvent::data.name}.${OrderSideMatch::taker.name}",
            source,
            startDate,
            endDate
        )
    }

    fun getTokenPurchaseAggregation(startDate: Date, endDate: Date, source: HistorySource?): Flux<AggregatedData> {
        return getNftPurchaseAggregation(
            "${LogEvent::data.name}.${OrderSideMatch::make.name}.${Asset::type.name}.${NftAssetType::token.name}",
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
            (LogEvent::data / OrderExchangeHistory::make / Asset::type / AssetType::nft  isEqualTo true)
                .and(LogEvent::data / OrderExchangeHistory::type).isEqualTo(ItemType.ORDER_SIDE_MATCH)
                .and(LogEvent::data / OrderExchangeHistory::date).gt(startDate).lt(endDate)
                .and(LogEvent::status).inValues(LogEventStatus.PENDING, LogEventStatus.CONFIRMED)
                .run { source?.let { and(LogEvent::data / OrderExchangeHistory::source).isEqualTo(it) } ?: this }
        )
        val group = Aggregation
            .group(groupByFiled)
            .sum("${LogEvent::data.name}.${OrderSideMatch::takeUsd.name}").`as`(AggregatedData::sum.name)
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
                "${LogEvent::data.name}.${OrderExchangeHistory::hash.name}",
                LogEvent::blockNumber.name,
                LogEvent::logIndex.name,
                LogEvent::minorLogIndex.name
            )

        val logger: Logger = LoggerFactory.getLogger(ExchangeHistoryRepository::class.java)
    }
}
