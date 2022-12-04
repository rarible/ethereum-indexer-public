package com.rarible.protocol.order.api.controller

import com.rarible.protocol.dto.ActivitiesByIdRequestDto
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.IdsDto
import com.rarible.protocol.dto.OrderActivitiesDto
import com.rarible.protocol.dto.OrderActivityFilterDto
import com.rarible.protocol.dto.SyncSortDto
import com.rarible.protocol.dto.mapper.ContinuationMapper
import com.rarible.protocol.order.api.converter.ActivityHistoryFilterConverter
import com.rarible.protocol.order.api.converter.ActivityVersionFilterConverter
import com.rarible.protocol.order.api.converter.ContinuationConverter
import com.rarible.protocol.order.api.service.activity.OrderActivityService
import com.rarible.protocol.order.core.continuation.page.PageSize
import com.rarible.protocol.order.core.converters.dto.OrderActivityConverter
import com.rarible.protocol.order.core.converters.model.ActivitySortConverter
import com.rarible.protocol.order.core.converters.model.ActivitySyncSortConverter
import com.rarible.protocol.order.core.model.ActivitySort
import com.rarible.protocol.order.core.repository.exchange.ActivityExchangeHistoryFilter
import com.rarible.protocol.order.core.repository.order.ActivityOrderVersionFilter
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderActivityController(
    private val orderActivityService: OrderActivityService,
    private val orderActivityConverter: OrderActivityConverter,
    private val historyFilterConverter: ActivityHistoryFilterConverter,
    private val versionFilterConverter: ActivityVersionFilterConverter
) : OrderActivityControllerApi {

    override suspend fun getOrderActivities(
        filter: OrderActivityFilterDto,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<OrderActivitiesDto> {
        val requestSize = PageSize.ORDER_ACTIVITY.limit(size)
        val continuationDto = ContinuationMapper.toActivityContinuationDto(continuation)
        val activitySort = sort?.let { ActivitySortConverter.convert(sort) } ?: ActivitySort.LATEST_FIRST
        val historyFilters = historyFilterConverter.convert(filter, activitySort, continuationDto)
        val versionFilters = versionFilterConverter.convert(filter, activitySort, continuationDto)

        val result = orderActivityService
            .search(historyFilters, versionFilters, activitySort, requestSize)
            .mapNotNull { orderActivityConverter.convert(it) }

        val nextContinuation = if (result.isEmpty() || result.size < requestSize) {
            null
        } else {
            ContinuationMapper.toString(result.last())
        }
        val orderActivities = OrderActivitiesDto(nextContinuation, result)
        return ResponseEntity.ok(orderActivities)
    }

    override suspend fun getOrderActivitiesById(activitiesByIdRequestDto: ActivitiesByIdRequestDto): ResponseEntity<OrderActivitiesDto> {
        val ids = activitiesByIdRequestDto.ids.map { ObjectId(it) }
        val result = orderActivityService
            .findByIds(ids)
            .mapNotNull { orderActivityConverter.convert(it) }
        val orderActivities = OrderActivitiesDto(null, result)
        return ResponseEntity.ok(orderActivities)
    }

    override suspend fun getOrderActivitiesSync(
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?
    ): ResponseEntity<OrderActivitiesDto> {
        val requestSize = PageSize.ORDER_ACTIVITY.limit(size)
        val continuationDto = ContinuationMapper.toActivityContinuationDto(continuation)
        val activitySort = sort?.let { ActivitySyncSortConverter.convert(sort) } ?: ActivitySort.SYNC_EARLIEST_FIRST

        val historyFilter = ActivityExchangeHistoryFilter.AllSync(
            activitySort,
            continuationDto?.let { ContinuationConverter.convert(it) })

        val versionFilter = ActivityOrderVersionFilter.AllSync(
            activitySort,
            continuationDto?.let { ContinuationConverter.convert(it) })

        val result = orderActivityService
            .search(listOf(historyFilter), listOf(versionFilter), activitySort, requestSize)
            .mapNotNull { orderActivityConverter.convert(it) }

        val nextContinuation = if (result.isEmpty() || result.size < requestSize) {
            null
        } else {
            ContinuationMapper.toSyncString(result.last())
        }
        val orderActivities = OrderActivitiesDto(nextContinuation, result)
        return ResponseEntity.ok(orderActivities)
    }

    override suspend fun getOrderRevertedActivitiesSync(
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?
    ): ResponseEntity<OrderActivitiesDto> {
        val requestSize = PageSize.ORDER_ACTIVITY.limit(size)
        val continuationDto = ContinuationMapper.toActivityContinuationDto(continuation)
        val activitySort = sort?.let { ActivitySyncSortConverter.convert(sort) } ?: ActivitySort.SYNC_EARLIEST_FIRST

        val historyFilter = ActivityExchangeHistoryFilter.AllRevertedSync(
            activitySort,
            continuationDto?.let { ContinuationConverter.convert(it) })

        val result = orderActivityService
            .search(listOf(historyFilter), emptyList(), activitySort, requestSize)
            .mapNotNull { orderActivityConverter.convert(it) }

        val nextContinuation = if (result.isEmpty() || result.size < requestSize) {
            null
        } else {
            ContinuationMapper.toSyncString(result.last())
        }
        val orderActivities = OrderActivitiesDto(nextContinuation, result)
        return ResponseEntity.ok(orderActivities)
    }

    override suspend fun getOrderSellRightActivities(
        continuation: String?,
        size: Int?
    ): ResponseEntity<IdsDto> {
        val requestSize = PageSize.ORDER_ACTIVITY.limit(size)
        val continuationDto = ContinuationMapper.toActivityContinuationDto(continuation)
        val historyFilter = ActivityExchangeHistoryFilter.AllSellRight(
            ActivitySort.SYNC_EARLIEST_FIRST,
            continuationDto?.let { ContinuationConverter.convert(it) })
        val result = orderActivityService
            .searchRight(historyFilter, ActivitySort.LATEST_FIRST, requestSize)
            .map { it.toString() }

        val nextContinuation = if (result.isEmpty() || result.size < requestSize) {
            null
        } else {
            "0_${result.last()}"
        }
        val idsDto = IdsDto(nextContinuation, result)
        return ResponseEntity.ok(idsDto)
    }
}
