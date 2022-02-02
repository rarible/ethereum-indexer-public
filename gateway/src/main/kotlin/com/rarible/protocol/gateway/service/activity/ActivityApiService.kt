package com.rarible.protocol.gateway.service.activity

import com.rarible.protocol.dto.ActivitiesDto
import com.rarible.protocol.dto.ActivityDto
import com.rarible.protocol.dto.ActivityFilterDto
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.mapper.ContinuationMapper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.lang.Integer.min
import java.util.Comparator

@Component
class ActivityApiService(
    private val activityService: ActivityService
) {
    private val logger = LoggerFactory.getLogger(ActivityApiService::class.java)

    private companion object {
        const val DEFAULT_SIZE = 50
        private val ACTIVITY_COMPARATOR = compareByDescending(ActivityDto::date)
            .then(compareByDescending(ActivityDto::id))

        fun getComparator(sort: ActivitySortDto?): Comparator<ActivityDto> =
            if (sort == ActivitySortDto.EARLIEST_FIRST) {
                ACTIVITY_COMPARATOR.reversed()
            } else {
                ACTIVITY_COMPARATOR
            }
    }

    suspend fun getActivities(
        filter: ActivityFilterDto,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ActivitiesDto = coroutineScope {
        logger.debug(
            "Searching for Activities with params: filter=[{}], continuation={}, size={}",
            filter, continuation, size
        )
        val requestSize = min(size ?: DEFAULT_SIZE, DEFAULT_SIZE)

        val nftResults = async { activityService.getNftActivities(filter, continuation, requestSize, sort) }
        val orderResults = async { activityService.getOrderActivities(filter, continuation, requestSize, sort) }

        logger.debug(
            "Found Activities: {} from NFT-Indexer and {} from Order-Indexer",
            nftResults.await().items.size, orderResults.await().items.size
        )
        val activities = (nftResults.await().items + orderResults.await().items)
            .sortedWith(getComparator(sort))
            .take(requestSize)

        val hasMore = (activities.isNotEmpty() && activities.size >= requestSize)
        val nextContinuation = if (hasMore) ContinuationMapper.toString(activities.last()) else null

        ActivitiesDto(nextContinuation, activities)
    }
}
