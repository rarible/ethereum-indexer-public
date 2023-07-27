package com.rarible.protocol.nft.api.controller

import com.rarible.protocol.dto.ActivitiesByIdRequestDto
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.NftActivitiesDto
import com.rarible.protocol.dto.NftActivityFilterDto
import com.rarible.protocol.dto.SyncSortDto
import com.rarible.protocol.dto.mapper.ContinuationMapper
import com.rarible.protocol.nft.api.converter.ActivityHistoryFilterConverter
import com.rarible.protocol.nft.api.converter.ContinuationConverter
import com.rarible.protocol.nft.api.service.activity.NftActivityService
import com.rarible.protocol.nft.core.converters.dto.NftActivityConverter
import com.rarible.protocol.nft.core.converters.model.ActivitySortConverter
import com.rarible.protocol.nft.core.converters.model.ActivitySyncSortConverter
import com.rarible.protocol.nft.core.page.PageSize
import com.rarible.protocol.nft.core.repository.history.ActivityItemHistoryFilter
import com.rarible.protocol.nft.core.repository.history.ActivitySort
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ActivityController(
    private val nftActivityService: NftActivityService,
    private val historyFilterConverter: ActivityHistoryFilterConverter,
    private val nftActivityConverter: NftActivityConverter
) : NftActivityControllerApi {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getNftActivities(
        request: NftActivityFilterDto,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<NftActivitiesDto> {
        val requestSize = PageSize.ITEM_ACTIVITY.limit(size)
        val continuationDto = ContinuationMapper.toActivityContinuationDto(continuation)
        val activitySort = sort?.let { ActivitySortConverter.convert(it) } ?: ActivitySort.LATEST_FIRST
        val historyFilters = historyFilterConverter.convert(activitySort, request, continuationDto)
        logger.info("Filters: ${historyFilters.joinToString { it.javaClass.simpleName }}")

        val result = nftActivityService
            .search(historyFilters, activitySort, requestSize)
            .mapNotNull { nftActivityConverter.convert(it.value) }

        val nextContinuation = if (result.isEmpty() || result.size < requestSize) {
            null
        } else {
            ContinuationMapper.toString(result.last())
        }
        return ResponseEntity.ok(NftActivitiesDto(nextContinuation, result))
    }

    override suspend fun getNftActivitiesById(nftActivitiesByIdRequestDto: ActivitiesByIdRequestDto): ResponseEntity<NftActivitiesDto> {
        val objectIds = nftActivitiesByIdRequestDto.ids.map { ObjectId(it) }.toSet()
        val activities = nftActivityService
            .findByIds(objectIds)
            .mapNotNull { nftActivityConverter.convert(it) }

        return ResponseEntity.ok(NftActivitiesDto(null, activities))
    }

    override suspend fun getNftActivitiesSync(
        reverted: Boolean?,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?,
    ): ResponseEntity<NftActivitiesDto> {
        val requestSize = PageSize.ITEM_ACTIVITY.limit(size)
        val continuationDto = ContinuationMapper.toActivityContinuationDto(continuation)
        val activitySort = sort?.let { ActivitySyncSortConverter.convert(it) } ?: ActivitySort.SYNC_EARLIEST_FIRST
        val activityFilter =
            ActivityItemHistoryFilter.AllSync(
                sort = activitySort,
                continuation = continuationDto?.let { ContinuationConverter.convert(it) },
                reverted = reverted ?: false
            )

        val result = nftActivityService
            .search(listOf(activityFilter), activitySort, requestSize)
            .mapNotNull { nftActivityConverter.convert(it.value) }
        val nextContinuation = if (result.isEmpty() || result.size < requestSize) {
            null
        } else {
            ContinuationMapper.toSyncString(result.last())
        }

        return ResponseEntity.ok(NftActivitiesDto(nextContinuation, result))
    }
}
