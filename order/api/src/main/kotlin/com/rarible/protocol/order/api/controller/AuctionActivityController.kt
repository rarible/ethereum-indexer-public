package com.rarible.protocol.order.api.controller

import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.AuctionActivitiesDto
import com.rarible.protocol.dto.AuctionActivityFilterDto
import com.rarible.protocol.dto.mapper.ContinuationMapper
import com.rarible.protocol.order.api.converter.AuctionHistoryFilterConverter
import com.rarible.protocol.order.api.service.activity.AuctionActivityService
import com.rarible.protocol.order.core.continuation.page.PageSize
import com.rarible.protocol.order.core.converters.dto.AuctionActivityConverter
import com.rarible.protocol.order.core.converters.model.ActivitySortConverter
import com.rarible.protocol.order.core.model.ActivityResult
import com.rarible.protocol.order.core.model.ActivitySort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AuctionActivityController(
    private val auctionActivityService: AuctionActivityService,
    private val auctionHistoryFilterConverter: AuctionHistoryFilterConverter,
    private val auctionActivityConverter: AuctionActivityConverter
) : AuctionActivityControllerApi {

    override suspend fun getAuctionActivities(
        filter: AuctionActivityFilterDto,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<AuctionActivitiesDto> {
        val requestSize = PageSize.AUCTION_ACTIVITY.limit(size)
        val activitySort = sort?.let { ActivitySortConverter.convert(sort) } ?: ActivitySort.LATEST_FIRST
        val historyFilters = auctionHistoryFilterConverter.convert(filter, activitySort, continuation)
        val result = auctionActivityService.search(historyFilters, activitySort, requestSize)
            .mapNotNull {
                auctionActivityConverter.convert((it as ActivityResult.History).value)
            }
        val nextContinuation = if (result.isEmpty() || result.size < requestSize) {
            null
        } else {
            ContinuationMapper.toString(result.last())
        }
        val auctionActivities = AuctionActivitiesDto(nextContinuation, result)
        return ResponseEntity.ok(auctionActivities)
    }
}
