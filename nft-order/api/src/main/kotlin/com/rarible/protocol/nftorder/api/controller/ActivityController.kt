package com.rarible.protocol.nftorder.api.controller

import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.api.service.ActivityApiService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address
import java.math.BigInteger

@RestController
class ActivityController(
    private val activityApiService: ActivityApiService
) : NftOrderActivityControllerApi {

    override suspend fun getNftOrderAllActivities(
        type: List<String>,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val filter = ActivityFilterAllDto(
            LinkedHashSet(type).map { ActivityFilterAllDto.Types.valueOf(it) }
        )
        val result = activityApiService.getActivities(filter, continuation, size, sort)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOrderActivitiesByItem(
        type: List<String>,
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val filter = ActivityFilterByItemDto(
            Address.apply(contract),
            BigInteger(tokenId),
            LinkedHashSet(type).map { ActivityFilterByItemDto.Types.valueOf(it) }
        )
        val result = activityApiService.getActivities(filter, continuation, size, sort)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOrderActivitiesByCollection(
        type: List<String>,
        collection: String,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val filter = ActivityFilterByCollectionDto(
            Address.apply(collection),
            LinkedHashSet(type).map { ActivityFilterByCollectionDto.Types.valueOf(it) }
        )
        val result = activityApiService.getActivities(filter, continuation, size, sort)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOrderActivitiesByUser(
        type: List<String>,
        user: List<Address>,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val filter = ActivityFilterByUserDto(
            user,
            LinkedHashSet(type).map { ActivityFilterByUserDto.Types.valueOf(it) }
        )
        val result = activityApiService.getActivities(filter, continuation, size, sort)
        return ResponseEntity.ok(result)
    }
}
