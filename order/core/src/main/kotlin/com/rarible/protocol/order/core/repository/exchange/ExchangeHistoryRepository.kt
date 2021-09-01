package com.rarible.protocol.order.core.repository.exchange

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepositoryIndexes.ALL_INDEXES
import com.rarible.protocol.order.core.repository.exchange.misc.aggregateWithHint
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

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

    fun findByItemType(type: ItemType): Flux<LogEvent> {
        val query = Query(LogEvent::topic inValues type.topic)
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

    fun searchActivity(filter: ActivityExchangeHistoryFilter): Flux<LogEvent> {
        val hint = filter.hint
        val criteria = filter.getCriteria()
            .and(LogEvent::status).isEqualTo(LogEventStatus.CONFIRMED)

        val query = Query(criteria)

        if (hint != null) {
            query.withHint(hint)
        }
        return template.find(query.with(ACTIVITY_SORT), LogEvent::class.java, COLLECTION)
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
            "${LogEvent::data.name}.${OrderSideMatch::make.name}.${Asset::type.name}.${Erc721AssetType::token.name}",
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
                .and(LogEvent::data / OrderExchangeHistory::date).gt(startDate).lt(endDate)
                .and(LogEvent::status).inValues(LogEventStatus.PENDING, LogEventStatus.CONFIRMED)
                .and(LogEvent::data / OrderExchangeHistory::type).isEqualTo(ItemType.ORDER_SIDE_MATCH)
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

        val ACTIVITY_SORT: Sort = Sort.by(
            Sort.Order.desc("${LogEvent::data.name}.${OrderExchangeHistory::date.name}"),
            Sort.Order.desc(OrderVersion::id.name)
        )

        val LOG_SORT_ASC: Sort = Sort
            .by(
                "${LogEvent::data.name}.${OrderExchangeHistory::hash.name}",
                LogEvent::blockNumber.name,
                LogEvent::logIndex.name
            )

        val logger: Logger = LoggerFactory.getLogger(ExchangeHistoryRepository::class.java)
    }
}
