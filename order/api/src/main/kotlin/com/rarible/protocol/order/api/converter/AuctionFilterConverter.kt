package com.rarible.protocol.order.api.converter

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.AuctionActivityFilterAllDto
import com.rarible.protocol.dto.AuctionActivityFilterByCollectionDto
import com.rarible.protocol.dto.AuctionActivityFilterByItemDto
import com.rarible.protocol.dto.AuctionActivityFilterByUserDto
import com.rarible.protocol.dto.AuctionActivityFilterDto
import com.rarible.protocol.order.core.model.AuctionActivitySort
import com.rarible.protocol.order.core.model.AuctionHistoryType
import com.rarible.protocol.order.core.repository.auction.ActivityAuctionHistoryFilter
import com.rarible.protocol.order.core.repository.auction.AuctionByCollection
import com.rarible.protocol.order.core.repository.auction.AuctionByItem
import com.rarible.protocol.order.core.repository.auction.AuctionByUser
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class AuctionHistoryFilterConverter {

    fun convert(
        source: AuctionActivityFilterDto,
        sort: AuctionActivitySort,
        continuation: String?
    ): List<ActivityAuctionHistoryFilter> {
        return when (source) {
            is AuctionActivityFilterAllDto -> source.types.flatMap {
                when (it) {
                    AuctionActivityFilterAllDto.Types.CREATED -> listOf(ActivityAuctionHistoryFilter.AuctionAllByType(AuctionHistoryType.ON_CHAIN_AUCTION, continuation, sort))
                    AuctionActivityFilterAllDto.Types.BID -> listOf(ActivityAuctionHistoryFilter.AuctionAllByType(AuctionHistoryType.BID_PLACED, continuation, sort))
                    AuctionActivityFilterAllDto.Types.CANCEL -> listOf(ActivityAuctionHistoryFilter.AuctionAllByType(AuctionHistoryType.AUCTION_CANCELLED, continuation, sort))
                    AuctionActivityFilterAllDto.Types.FINISHED -> listOf(ActivityAuctionHistoryFilter.AuctionAllByType(AuctionHistoryType.AUCTION_FINISHED, continuation, sort))
                    else -> emptyList()
                }
            }
            is AuctionActivityFilterByUserDto -> source.types.flatMap {
                val from = source.from?.let { from -> Instant.ofEpochSecond(from) }
                val to = source.to?.let { to -> Instant.ofEpochSecond(to) }
                when (it) {
                    AuctionActivityFilterByUserDto.Types.CREATED -> listOf(AuctionByUser.Created(source.users ?: emptyList(), from, to, continuation, sort))
                    AuctionActivityFilterByUserDto.Types.BID -> listOf(AuctionByUser.Bid(source.users ?: emptyList(), from, to, continuation, sort))
                    AuctionActivityFilterByUserDto.Types.CANCEL -> listOf(AuctionByUser.Cancel(source.users ?: emptyList(), from, to, continuation, sort))
                    else -> emptyList()
                }
            }
            is AuctionActivityFilterByItemDto -> source.types.flatMap {
                when (it) {
                    AuctionActivityFilterByItemDto.Types.CREATED -> listOf(AuctionByItem.Created(source.contract, EthUInt256.of(source.tokenId), continuation, sort))
                    AuctionActivityFilterByItemDto.Types.BID -> listOf(AuctionByItem.Bid(source.contract, EthUInt256.of(source.tokenId), continuation, sort))
                    AuctionActivityFilterByItemDto.Types.CANCEL -> listOf(AuctionByItem.Cancel(source.contract, EthUInt256.of(source.tokenId), continuation, sort))
                    AuctionActivityFilterByItemDto.Types.FINISHED -> listOf(AuctionByItem.Finished(source.contract, EthUInt256.of(source.tokenId), continuation, sort))
                    else -> emptyList()
                }
            }
            is AuctionActivityFilterByCollectionDto -> source.types.flatMap {
                when (it) {
                    AuctionActivityFilterByCollectionDto.Types.CREATED -> listOf(AuctionByCollection.Created(source.contract, continuation, sort))
                    AuctionActivityFilterByCollectionDto.Types.BID -> listOf(AuctionByCollection.Bid(source.contract, continuation, sort))
                    AuctionActivityFilterByCollectionDto.Types.CANCEL -> listOf(AuctionByCollection.Cancel(source.contract, continuation, sort))
                    AuctionActivityFilterByCollectionDto.Types.FINISHED -> listOf(AuctionByCollection.Finished(source.contract, continuation, sort))
                    else -> emptyList()
                }
            }
        }
    }
}
