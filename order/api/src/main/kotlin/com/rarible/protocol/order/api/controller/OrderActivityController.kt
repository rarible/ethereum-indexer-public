package com.rarible.protocol.order.api.controller

import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.OrderActivitiesDto
import com.rarible.protocol.dto.OrderActivityFilterDto
import com.rarible.protocol.dto.mapper.ContinuationMapper
import com.rarible.protocol.order.api.converter.ActivityHistoryFilterConverter
import com.rarible.protocol.order.api.converter.ActivityVersionFilterConverter
import com.rarible.protocol.order.core.converters.dto.OrderActivityConverter
import com.rarible.protocol.order.api.service.activity.OrderActivityService
import com.rarible.protocol.order.core.converters.model.ActivitySortConverter
import com.rarible.protocol.order.core.misc.limit
import com.rarible.protocol.order.core.model.ActivitySort
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
        val requestSize = size.limit()
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
}
