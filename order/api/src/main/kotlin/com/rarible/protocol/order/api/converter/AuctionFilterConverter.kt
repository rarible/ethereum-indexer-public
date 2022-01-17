package com.rarible.protocol.order.api.converter

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.api.configuration.OrderIndexerApiProperties
import com.rarible.protocol.order.core.model.ActivitySort
import com.rarible.protocol.order.core.model.AuctionHistoryType
import com.rarible.protocol.order.core.repository.auction.ActivityAuctionHistoryFilter
import com.rarible.protocol.order.core.repository.auction.AuctionByItem
import com.rarible.protocol.order.core.repository.auction.AuctionByUser
import org.springframework.stereotype.Component

@Component
class AuctionHistoryFilterConverter(properties: OrderIndexerApiProperties) {

    fun convert(
        source: AuctionActivityFilterDto,
        sort: ActivitySort,
        continuation: String?
    ): List<ActivityAuctionHistoryFilter> {
        return when (source) {
            is AuctionActivityFilterAllDto -> source.types.flatMap {
                when (it) {
                    AuctionActivityFilterAllDto.Types.CREATED -> listOf(ActivityAuctionHistoryFilter.AuctionByType(AuctionHistoryType.ON_CHAIN_AUCTION))
                    AuctionActivityFilterAllDto.Types.BID -> listOf(ActivityAuctionHistoryFilter.AuctionByType(AuctionHistoryType.BID_PLACED))
                    AuctionActivityFilterAllDto.Types.CANCEL -> listOf(ActivityAuctionHistoryFilter.AuctionByType(AuctionHistoryType.AUCTION_CANCELLED))
                    AuctionActivityFilterAllDto.Types.FINISHED -> listOf(ActivityAuctionHistoryFilter.AuctionByType(AuctionHistoryType.AUCTION_FINISHED))
                }
            }
            is AuctionActivityFilterByUserDto -> source.types.flatMap {
                when (it) {
                    AuctionActivityFilterByUserDto.Types.CREATED -> listOf(AuctionByUser.Created(source.user, continuation))
                    AuctionActivityFilterByUserDto.Types.BID -> listOf(AuctionByUser.Bid(source.user, continuation))
                    AuctionActivityFilterByUserDto.Types.CANCEL -> emptyList()
                    AuctionActivityFilterByUserDto.Types.FINISHED -> listOf(AuctionByUser.Finished(source.user, continuation))
                }
            }
            is AuctionActivityFilterByItemDto -> source.types.flatMap {
                when (it) {
                    AuctionActivityFilterByItemDto.Types.CREATED -> listOf(AuctionByItem.Created(source.contract, EthUInt256.of(source.tokenId), continuation))
                    AuctionActivityFilterByItemDto.Types.BID -> listOf(AuctionByItem.Bid(source.contract, EthUInt256.of(source.tokenId), continuation))
                    AuctionActivityFilterByItemDto.Types.CANCEL -> listOf(AuctionByItem.Cancel(source.contract, EthUInt256.of(source.tokenId), continuation))
                    AuctionActivityFilterByItemDto.Types.FINISHED -> listOf(AuctionByItem.Finished(source.contract, EthUInt256.of(source.tokenId), continuation))
                }
            }
            is AuctionActivityFilterByCollectionDto -> TODO()
        }
    }
}
