package com.rarible.protocol.order.api.controller

import com.rarible.protocol.dto.AggregationDataDto
import com.rarible.protocol.dto.OrderCollectionStatsDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.dto.parser.AddressParser
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.order.api.service.aggregation.OrderAggregationService
import com.rarible.protocol.order.core.continuation.page.PageSize
import com.rarible.protocol.order.core.converters.dto.AggregationDataDtoConverter
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.service.CollectionOrderStatService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.*

@RestController
class OrderAggregationController(
    private val orderAggregationService: OrderAggregationService,
    private val collectionOrderStatService: CollectionOrderStatService,
    private val nftCollectionClient: NftCollectionControllerApi
) : OrderAggregationControllerApi {

    override fun aggregateNftSellByMaker(
        startDate: Long,
        endDate: Long,
        size: Long?,
        source: PlatformDto?
    ): ResponseEntity<Flow<AggregationDataDto>> {
        val result = orderAggregationService
            .getNftSellOrdersAggregationByMaker(Date(startDate), Date(endDate), convert(source))
            .take(limit(size))
            .map { AggregationDataDtoConverter.convert(it) }
            .asFlow()
        return ResponseEntity.ok(result)
    }

    override fun aggregateNftPurchaseByTaker(
        startDate: Long,
        endDate: Long,
        size: Long?,
        source: PlatformDto?
    ): ResponseEntity<Flow<AggregationDataDto>> {
        val result = orderAggregationService
            .getNftPurchaseOrdersAggregationByTaker(Date(startDate), Date(endDate), convert(source))
            .take(limit(size))
            .map { AggregationDataDtoConverter.convert(it) }
            .asFlow()
        return ResponseEntity.ok(result)
    }

    // TODO remove later
    override suspend fun aggregateNftCollectionStats(collection: String): ResponseEntity<OrderCollectionStatsDto> {

        nftCollectionClient.getNftCollectionById(collection).awaitFirst() // To throw 404 if not found
        val stat = collectionOrderStatService.getOrSchedule(AddressParser.parse(collection))

        // Initial stat record, not filled with real data yet
        val result = if (stat.lastUpdatedAt == Instant.EPOCH) {
            OrderCollectionStatsDto(token = stat.id)
        } else {
            OrderCollectionStatsDto(
                token = stat.id,
                floorPrice = stat.floorPrice,
                highestSale = stat.highestSale,
                totalVolume = stat.totalVolume,
                lastUpdatedAt = stat.lastUpdatedAt
            )
        }
        return ResponseEntity.ok(result)
    }

    override fun aggregateNftPurchaseByCollection(
        startDate: Long,
        endDate: Long,
        size: Long?,
        source: PlatformDto?
    ): ResponseEntity<Flow<AggregationDataDto>> {
        val result = orderAggregationService
            .getNftPurchaseOrdersAggregationByCollection(Date(startDate), Date(endDate), convert(source))
            .take(limit(size))
            .map { AggregationDataDtoConverter.convert(it) }
            .asFlow()
        return ResponseEntity.ok(result)
    }

    private fun convert(source: PlatformDto?): HistorySource? {
        return when (source) {
            PlatformDto.RARIBLE -> HistorySource.RARIBLE
            PlatformDto.OPEN_SEA -> HistorySource.OPEN_SEA
            else -> null
        }
    }

    private fun limit(size: Long?): Long = PageSize.ORDER_AGGREGATION.limit(size)
}

