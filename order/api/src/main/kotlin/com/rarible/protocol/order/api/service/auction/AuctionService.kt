package com.rarible.protocol.order.api.service.auction

import com.rarible.protocol.order.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.repository.auction.AuctionFilter
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component

@Component
class AuctionService(
    private val auctionRepository: AuctionRepository
) {
    suspend fun get(hash: Word): Auction {
        return auctionRepository.findById(hash) ?: throw EntityNotFoundApiException("Auction", hash)
    }

    fun getAll(hashes: List<Word>): Flow<Auction> {
        return auctionRepository.findAll(hashes)
    }

    suspend fun search(filter: AuctionFilter, size: Int, continuation: String?): List<Auction> {
        return auctionRepository.search(filter, size, continuation)
    }
}
