package com.rarible.protocol.order.api.controller

import com.rarible.core.common.convert
import com.rarible.protocol.dto.AggregationDataDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.api.service.aggregation.OrderAggregationService
import com.rarible.protocol.order.core.model.HistorySource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.core.convert.ConversionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.lang.Long.min
import java.util.*

@RestController
class OrderAggregationController(
    private val orderAggregationService: OrderAggregationService,
    private val conversionService: ConversionService
) : OrderAggregationControllerApi {

    override fun aggregateNftSellByMaker(
        startDate: Long,
        endDate: Long,
        size: Long?,
        source: PlatformDto?
    ): ResponseEntity<Flow<AggregationDataDto>> {
        val result = orderAggregationService
            .getNftSellOrdersAggregationByMaker(Date(startDate), Date(endDate), convert(source))
            .take(size.limit())
            .map { conversionService.convert<AggregationDataDto>(it) }
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
            .take(size.limit())
            .map { conversionService.convert<AggregationDataDto>(it) }
            .asFlow()
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
            .take(size.limit())
            .map { conversionService.convert<AggregationDataDto>(it) }
            .asFlow()
        return ResponseEntity.ok(result)
    }

    private fun convert(source: PlatformDto?): HistorySource? {
        return when (source) {
            PlatformDto.RARIBLE -> HistorySource.RARIBLE
            PlatformDto.OPEN_SEA -> HistorySource.OPEN_SEA
            PlatformDto.ALL -> null
            else -> HistorySource.RARIBLE
        }
    }

    companion object {
        private const val DEFAULT_SIZE = Long.MAX_VALUE
        private fun Long?.limit() = min(this ?: DEFAULT_SIZE, DEFAULT_SIZE)
    }
}

