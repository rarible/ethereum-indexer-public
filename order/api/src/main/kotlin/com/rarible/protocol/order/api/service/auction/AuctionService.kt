package com.rarible.protocol.order.api.service.auction

import com.rarible.protocol.order.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.auction.ActivityAuctionHistoryFilter
import com.rarible.protocol.order.core.repository.auction.AuctionFilter
import com.rarible.protocol.order.core.repository.auction.AuctionHistoryRepository
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component

@Component
class AuctionService(
    private val auctionRepository: AuctionRepository,
    private val auctionHistoryRepository: AuctionHistoryRepository
) {
    suspend fun get(hash: Word): Auction {
        return auctionRepository.findById(hash) ?: throw EntityNotFoundApiException("Auction", hash)
    }

    suspend fun getAuctionBids(hash: Word, continuation: String?, size: Int): List<AuctionBidEntity> {
        val auction = get(hash)
        val filter = ActivityAuctionHistoryFilter.AllAuctionBids(hash, continuation)

        return auctionHistoryRepository
            .searchActivity(filter, size)
            .map { logs ->
                val bidPlaced = logs.data as BidPlaced
                AuctionBidEntity(
                    id = logs.id.toHexString(),
                    bid = bidPlaced.bid,
                    buyer = bidPlaced.buyer,
                    buy = auction.buy
                )
            }
            .collectList().awaitFirst()
    }

    fun getAll(hashes: List<Word>): Flow<Auction> {
        return auctionRepository.findAll(hashes)
    }

    suspend fun search(filter: AuctionFilter, size: Int, continuation: String?): List<Auction> {
        return auctionRepository.search(filter, size, continuation)
    }
}
