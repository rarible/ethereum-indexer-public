package com.rarible.protocol.order.api.converter

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.AuctionActivityFilterAllDto
import com.rarible.protocol.dto.AuctionActivityFilterByCollectionDto
import com.rarible.protocol.dto.AuctionActivityFilterByItemDto
import com.rarible.protocol.dto.AuctionActivityFilterByUserDto
import com.rarible.protocol.dto.AuctionActivityFilterDto
import com.rarible.protocol.order.core.model.AuctionActivitySort
import com.rarible.protocol.order.core.model.AuctionOffchainHistory
import com.rarible.protocol.order.core.repository.auction.ActivityAuctionOffchainFilter
import com.rarible.protocol.order.core.repository.auction.AuctionOffchainByCollection
import com.rarible.protocol.order.core.repository.auction.AuctionOffchainByItem
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
            is AuctionActivityFilterByUserDto -> emptyList()
            is AuctionActivityFilterByItemDto -> source.types.flatMap {
                when (it) {
                    AuctionActivityFilterByItemDto.Types.STARTED -> listOf(AuctionOffchainByItem.Started(source.contract, EthUInt256.of(source.tokenId), continuation, sort))
                    AuctionActivityFilterByItemDto.Types.ENDED -> listOf(AuctionOffchainByItem.Ended(source.contract, EthUInt256.of(source.tokenId), continuation, sort))
                    else -> listOf()
                }
            }
            is AuctionActivityFilterByCollectionDto -> source.types.flatMap {
                when (it) {
                    AuctionActivityFilterByCollectionDto.Types.STARTED -> listOf(AuctionOffchainByCollection.Started(source.contract, continuation, sort))
                    AuctionActivityFilterByCollectionDto.Types.ENDED -> listOf(AuctionOffchainByCollection.Ended(source.contract, continuation, sort))
                    else -> listOf()
                }
            }
        }
    }
}
