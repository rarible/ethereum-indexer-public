package com.rarible.protocol.order.api.controller

import com.rarible.protocol.dto.ActivitiesByIdRequestDto
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.AuctionActivitiesDto
import com.rarible.protocol.dto.AuctionActivitiesSyncTypesDto
import com.rarible.protocol.dto.AuctionActivityFilterDto
import com.rarible.protocol.dto.SyncSortDto
import com.rarible.protocol.dto.mapper.ContinuationMapper
import com.rarible.protocol.order.api.converter.AuctionHistoryFilterConverter
import com.rarible.protocol.order.api.converter.AuctionOffchainFilterConverter
import com.rarible.protocol.order.api.service.activity.AuctionActivityService
import com.rarible.protocol.order.core.continuation.page.PageSize
import com.rarible.protocol.order.core.converters.dto.AuctionActivityConverter
import com.rarible.protocol.order.core.converters.model.AuctionActivitySortConverter
import com.rarible.protocol.order.core.converters.model.AuctionActivitySyncSortConverter
import com.rarible.protocol.order.core.model.AuctionActivityResult
import com.rarible.protocol.order.core.model.AuctionActivitySort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AuctionActivityController(
    private val auctionActivityService: AuctionActivityService,
    private val auctionHistoryFilterConverter: AuctionHistoryFilterConverter,
    private val auctionOffchainFilterConverter: AuctionOffchainFilterConverter,
    private val auctionActivityConverter: AuctionActivityConverter
) : AuctionActivityControllerApi {

    override suspend fun getAuctionActivities(
        filter: AuctionActivityFilterDto,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<AuctionActivitiesDto> {
        val requestSize = PageSize.AUCTION_ACTIVITY.limit(size)
        val activitySort = sort?.let { AuctionActivitySortConverter.convert(sort) } ?: AuctionActivitySort.LATEST_FIRST
        val historyFilters = auctionHistoryFilterConverter.convert(filter, activitySort, continuation)
        val offchainFilters = auctionOffchainFilterConverter.convert(filter, activitySort, continuation)
        val result = auctionActivityService.search(historyFilters, offchainFilters, activitySort, requestSize)
            .mapNotNull {
                when(it) {
                    is AuctionActivityResult.History -> auctionActivityConverter.convert(it.value)
                    is AuctionActivityResult.OffchainHistory -> auctionActivityConverter.convert(it.value)
                }
            }
        val nextContinuation = if (result.isEmpty() || result.size < requestSize) {
            null
        } else {
            ContinuationMapper.toString(result.last())
        }
        val auctionActivities = AuctionActivitiesDto(nextContinuation, result)
        return ResponseEntity.ok(auctionActivities)
    }

    override suspend fun getAuctionActivitiesById(activitiesByIdRequestDto: ActivitiesByIdRequestDto): ResponseEntity<AuctionActivitiesDto> {
        val result = auctionActivityService
            .findByIds(activitiesByIdRequestDto.ids)
            .mapNotNull {
                when(it) {
                    is AuctionActivityResult.History -> auctionActivityConverter.convert(it.value)
                    is AuctionActivityResult.OffchainHistory -> auctionActivityConverter.convert(it.value)
                }
            }

        return ResponseEntity.ok(AuctionActivitiesDto(null, result))
    }

    override suspend fun getAuctionActivitiesSync(
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?,
        filter: List<AuctionActivitiesSyncTypesDto>?
    ): ResponseEntity<AuctionActivitiesDto> {
        val requestSize = PageSize.AUCTION_ACTIVITY.limit(size)
        val activitySort =
            sort?.let { AuctionActivitySyncSortConverter.convert(sort) } ?: AuctionActivitySort.SYNC_EARLIEST_FIRST
        val historyFilters = auctionHistoryFilterConverter.syncConvert(filter, activitySort, continuation)
        val offchainFilter =  auctionOffchainFilterConverter.syncConvert(filter, activitySort, continuation)
        val result = auctionActivityService.search(historyFilters, offchainFilter, activitySort, requestSize)
            .mapNotNull {
                when(it) {
                    is AuctionActivityResult.History -> auctionActivityConverter.convert(it.value)
                    is AuctionActivityResult.OffchainHistory -> auctionActivityConverter.convert(it.value)
                }
            }

        val nextContinuation = if (result.isEmpty() || result.size < requestSize) {
            null
        } else {
            ContinuationMapper.toSyncString(result.last())
        }

        val auctionActivities = AuctionActivitiesDto(nextContinuation, result)
        return ResponseEntity.ok(auctionActivities)
    }
}
