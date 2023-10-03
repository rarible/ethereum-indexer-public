package com.rarible.protocol.order.core.service

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.apm.withSpan
import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.CollectionOrderStat
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.order.OrderFilterSellByCollectionAndCurrency
import com.rarible.protocol.order.core.repository.CollectionOrderStatRepository
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigDecimal
import java.math.RoundingMode

@Component
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class CollectionOrderStatService(
    private val collectionStatRepository: CollectionOrderStatRepository,
    private val exchangeHistoryRepository: ExchangeHistoryRepository,
    private val orderRepository: OrderRepository,
    private val priceUpdateService: PriceUpdateService,
    private val currencyApi: CurrencyControllerApi,
    blockchain: Blockchain
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val blockchainDto = when (blockchain) {
        Blockchain.ETHEREUM -> BlockchainDto.ETHEREUM
        Blockchain.POLYGON -> BlockchainDto.POLYGON
        Blockchain.OPTIMISM -> BlockchainDto.OPTIMISM
        Blockchain.MANTLE -> BlockchainDto.MANTLE
    }

    val makeNftKey = ReversedEthereumLogRecord::data / OrderExchangeHistory::make / Asset::type / AssetType::nft
    val makeNftContractKey = ReversedEthereumLogRecord::data / OrderExchangeHistory::make / Asset::type / NftAssetType::token

    suspend fun getOrSchedule(token: Address, currency: String?): CollectionOrderStat {
        val stat = optimisticLock {
            val stat = collectionStatRepository.get(token)
            stat ?: collectionStatRepository.save(CollectionOrderStat.empty(token))
        }
        // No currency specified - return it as USD
        if (currency == null) {
            return stat
        }

        val rate = getRate(currency)
        return stat.copy(
            highestSale = applyRate(stat.highestSale, rate),
            totalVolume = applyRate(stat.totalVolume, rate),
            floorPrice = applyRate(stat.floorPrice, rate)
        )
    }

    suspend fun updateStat(token: Address, currencies: List<Address>): CollectionOrderStat {
        val result = coroutineScope {
            val sellStatsDeferred = async { evalSaleStats(token) }
            val floorPriceDeferred = async { evalFloorPrice(token, currencies) }
            val sellStats = sellStatsDeferred.await()
            CollectionOrderStat(
                id = token,
                lastUpdatedAt = nowMillis(),
                totalVolume = sellStats.totalVolume ?: BigDecimal.ZERO,
                highestSale = sellStats.highestSale ?: BigDecimal.ZERO,
                floorPrice = floorPriceDeferred.await() ?: BigDecimal.ZERO
            )
        }
        return optimisticLock {
            val exist = collectionStatRepository.get(token)
            val updated = collectionStatRepository.save(result.copy(version = exist?.version))
            logger.info(
                "Updated collection stat for {}: totalVolume = {}, highestSale = {}, floorPrice = {}",
                updated.id, updated.totalVolume, updated.highestSale, updated.floorPrice
            )
            updated
        }
    }

    private suspend fun evalSaleStats(token: Address): SalesStats {
        val match = Aggregation.match(
            makeNftKey.isEqualTo(true)
                .and(makeNftContractKey).isEqualTo(token)
                .and(ReversedEthereumLogRecord::data / OrderExchangeHistory::type).isEqualTo(ItemType.ORDER_SIDE_MATCH)
                .and(ReversedEthereumLogRecord::status).isEqualTo(EthereumBlockStatus.CONFIRMED)
        )
        val group = Aggregation
            .group("data.make.type.token")
            .sum("${ReversedEthereumLogRecord::data.name}.${OrderSideMatch::takeUsd.name}").`as`("totalVolume")
            .max("${ReversedEthereumLogRecord::data.name}.${OrderSideMatch::takeUsd.name}").`as`("highestSale")

        val aggregation = Aggregation.newAggregation(match, group)

        return exchangeHistoryRepository.aggregate(
            aggregation,
            ExchangeHistoryRepository.COLLECTION,
            SalesStats::class.java
        ).collectList().awaitFirst().firstOrNull() ?: SalesStats(token, BigDecimal.ZERO, BigDecimal.ZERO)
    }

    private suspend fun evalFloorPrice(token: Address, currencies: List<Address>): BigDecimal? {
        val bestOrders = coroutineScope {
            currencies.map {
                async { evalFloorPrice(token, it) }
            }.awaitAll().filterNotNull()
        }

        return bestOrders.mapNotNull {
            priceUpdateService.getAssetsUsdValue(it.make, it.take, nowMillis())?.makePriceUsd
        }.minOrNull()
    }

    private suspend fun evalFloorPrice(token: Address, currency: Address): Order? {
        val filter = OrderFilterSellByCollectionAndCurrency(
            contract = token,
            currency = currency
        )
        return orderRepository.search(filter.toQuery(null, 1)).firstOrNull()
    }

    private suspend fun getRate(currency: String): BigDecimal {
        return withSpan(name = "getCurrencyRate") {
            currencyApi.getCurrencyRate(blockchainDto, currency, nowMillis().toEpochMilli())
                .awaitFirstOrNull()?.rate
        } ?: BigDecimal.ZERO
    }

    private suspend fun applyRate(usd: BigDecimal, rate: BigDecimal): BigDecimal {
        if (rate.signum() == 0 || usd.signum() == 0) {
            return BigDecimal.ZERO
        }
        // 8 is additional scale for case if currency has very high rate
        return usd.divide(rate, usd.scale() + 8, RoundingMode.HALF_EVEN)
    }

    data class SalesStats(
        @Id
        val collection: Address,
        val totalVolume: BigDecimal?,
        val highestSale: BigDecimal?
    )
}
