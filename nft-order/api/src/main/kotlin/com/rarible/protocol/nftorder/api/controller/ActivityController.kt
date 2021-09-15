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
        type: List<ActivityFilterAllTypeDto>,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val filter = ActivityFilterAllDto(type)
        val result = activityApiService.getActivities(filter, continuation, size, sort)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOrderActivitiesByItem(
        type: List<ActivityFilterByItemTypeDto>,
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val filter = ActivityFilterByItemDto(Address.apply(contract), BigInteger(tokenId), type)
        val result = activityApiService.getActivities(filter, continuation, size, sort)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOrderActivitiesByCollection(
        type: List<ActivityFilterByCollectionTypeDto>,
        collection: String,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val filter = ActivityFilterByCollectionDto(Address.apply(collection), type)
        val result = activityApiService.getActivities(filter, continuation, size, sort)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOrderActivitiesByUser(
        type: List<ActivityFilterByUserTypeDto>,
        user: List<Address>,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val filter = ActivityFilterByUserDto(user, type)
        val result = activityApiService.getActivities(filter, continuation, size, sort)
        return ResponseEntity.ok(result)
    }
}
