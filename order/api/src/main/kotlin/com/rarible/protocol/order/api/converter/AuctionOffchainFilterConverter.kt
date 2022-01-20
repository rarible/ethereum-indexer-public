package com.rarible.protocol.order.api.converter

import com.rarible.protocol.dto.AuctionActivityFilterAllDto
import com.rarible.protocol.dto.AuctionActivityFilterByCollectionDto
import com.rarible.protocol.dto.AuctionActivityFilterByItemDto
import com.rarible.protocol.dto.AuctionActivityFilterByUserDto
import com.rarible.protocol.dto.AuctionActivityFilterDto
import com.rarible.protocol.order.core.model.AuctionActivitySort
import com.rarible.protocol.order.core.model.AuctionOffchainHistory
import com.rarible.protocol.order.core.repository.auction.ActivityAuctionOffchainFilter
import com.rarible.protocol.order.core.repository.auction.AuctionOffchainByUser
import org.springframework.stereotype.Component

@Component
class AuctionOffchainFilterConverter {

    fun convert(
        source: AuctionActivityFilterDto,
        sort: AuctionActivitySort,
        continuation: String?
    ): List<ActivityAuctionOffchainFilter> {
        return when (source) {
            is AuctionActivityFilterAllDto -> source.types.flatMap {
                when (it) {
                    AuctionActivityFilterAllDto.Types.STARTED -> listOf(ActivityAuctionOffchainFilter.AuctionAllByType(AuctionOffchainHistory.Type.STARTED, continuation, sort))
                    AuctionActivityFilterAllDto.Types.ENDED -> listOf(ActivityAuctionOffchainFilter.AuctionAllByType(AuctionOffchainHistory.Type.ENDED, continuation, sort))
                    else -> listOf()
                }
            }
            is AuctionActivityFilterByUserDto -> source.types.flatMap {
                when (it) {
                    AuctionActivityFilterByUserDto.Types.STARTED -> listOf(AuctionOffchainByUser.Started(source.user, continuation, sort))
                    AuctionActivityFilterByUserDto.Types.ENDED -> listOf(AuctionOffchainByUser.Ended(source.user, continuation, sort))
                    else -> listOf()
                }
            }
            is AuctionActivityFilterByItemDto -> source.types.flatMap {
                return listOf()
            }
            is AuctionActivityFilterByCollectionDto -> source.types.flatMap {
                return listOf()
            }
        }
    }
}
