package com.rarible.protocol.order.api.service.auction

import com.rarible.protocol.order.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.AuctionBids
import com.rarible.protocol.order.core.model.BidPlaced
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
    private val auctionRepository: AuctionRepository
    private val auctionHistoryRepository: AuctionHistoryRepository
) {
    suspend fun get(hash: Word): Auction {
        return auctionRepository.findById(hash) ?: throw EntityNotFoundApiException("Auction", hash)
    }

    suspend fun getAuctionBids(hash: Word, continuation: String?, size: Int): AuctionBids {
        val auction = get(hash)
        val filter = ActivityAuctionHistoryFilter.AllAuctionBids(hash, continuation)

        val bids = auctionHistoryRepository
            .searchActivity(filter, size)
            .map { logs -> logs.data as BidPlaced }
            .collectList().awaitFirst()

        return AuctionBids(bids, auction)
    }

    fun getAll(hashes: List<Word>): Flow<Auction> {
        return auctionRepository.findAll(hashes)
    }

    suspend fun search(filter: AuctionFilter, size: Int, continuation: String?): List<Auction> {
        return auctionRepository.search(filter, size, continuation)
    }
}
