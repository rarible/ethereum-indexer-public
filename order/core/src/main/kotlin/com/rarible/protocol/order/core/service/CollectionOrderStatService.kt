package com.rarible.protocol.order.core.service

import com.rarible.core.common.optimisticLock
import com.rarible.core.mongo.util.div
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.CollectionOrderStat
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.repository.CollectionOrderStatRepository
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigDecimal

@Component
class CollectionOrderStatService(
    private val collectionStatRepository: CollectionOrderStatRepository,
    private val exchangeHistoryRepository: ExchangeHistoryRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val makeNftKey = LogEvent::data / OrderExchangeHistory::make / Asset::type / AssetType::nft
    val makeNftContractKey = LogEvent::data / OrderExchangeHistory::make / Asset::type / NftAssetType::token

    suspend fun getOrSchedule(token: Address): CollectionOrderStat = optimisticLock {
        val stat = collectionStatRepository.get(token)
        stat ?: collectionStatRepository.save(CollectionOrderStat.empty(token))
    }

    suspend fun updateStat(token: Address): CollectionOrderStat {        // TODO add

    }

    private fun evalSales(token: Address): SalesStats? {
        val match = Aggregation.match(
            makeNftKey.isEqualTo(true)
                .and(makeNftContractKey).isEqualTo(token)
                .and(LogEvent::data / OrderExchangeHistory::type).isEqualTo(ItemType.ORDER_SIDE_MATCH)
                .and(LogEvent::status).isEqualTo(LogEventStatus.CONFIRMED)
        )
        val group = Aggregation
            .group("data.make.type.token")
            .sum("${LogEvent::data.name}.${OrderSideMatch::takeUsd.name}").`as`("totalVolume")
            .max("${LogEvent::data.name}.${OrderSideMatch::takeUsd.name}").`as`("highestSale")

        val aggregation = Aggregation.newAggregation(match, group)

        return exchangeHistoryRepository.aggregate(
            aggregation,
            ExchangeHistoryRepository.COLLECTION,
            SalesStats::class.java
        ).collectList().awaitFirst().firstOrNull()
    }

    data class SalesStats(
        @Id
        val collection: Address,
        val totalVolume: BigDecimal,
        val highestSale: Long
    )

}