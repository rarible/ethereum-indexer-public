package com.rarible.protocol.nftorder.core.service

import com.rarible.protocol.dto.ActivityFilterDto
import com.rarible.protocol.dto.NftActivitiesDto
import com.rarible.protocol.dto.OrderActivitiesDto
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.nftorder.core.converter.ActivityFilterDtoToNftDto
import com.rarible.protocol.nftorder.core.converter.ActivityFilterDtoToOrderDtoConverter
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component

@Component
class ActivityService(
    private val nftActivityControllerApi: NftActivityControllerApi,
    private val orderActivityControllerApi: OrderActivityControllerApi
) {

    companion object {
        private val EMPTY_ORDER_ACTIVITIES = OrderActivitiesDto(null, listOf())
        private val EMPTY_NFT_ACTIVITIES = NftActivitiesDto(null, listOf())
    }

    suspend fun getOrderActivities(
        filter: ActivityFilterDto,
        continuation: String?,
        size: Int,
        sort: String?
    ): OrderActivitiesDto {

        val convertedFilter = ActivityFilterDtoToOrderDtoConverter.convert(filter)
        return if (convertedFilter == null) {
            EMPTY_ORDER_ACTIVITIES
        } else {
            orderActivityControllerApi.getOrderActivities(convertedFilter, continuation, size, sort)
                .awaitFirst()
        }
    }

    suspend fun getNftActivities(
        filter: ActivityFilterDto,
        continuation: String?,
        size: Int,
        sort: String?
    ): NftActivitiesDto {

        val convertedFilter = ActivityFilterDtoToNftDto.convert(filter)
        return if (convertedFilter == null) {
            EMPTY_NFT_ACTIVITIES
        } else {
            nftActivityControllerApi.getNftActivities(convertedFilter, continuation, size, sort)
                .awaitFirst()
        }
    }
}