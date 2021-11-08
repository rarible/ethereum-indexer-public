package com.rarible.protocol.order.core.service.auction

import com.rarible.core.reduce.service.UpdateService
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import org.springframework.stereotype.Component

@Component
class AuctionUpdateService(
    private val auctionRepository: AuctionRepository
) : UpdateService<Auction> {
    override suspend fun update(data: Auction) {
        auctionRepository.save(data.withCalculatedState())
    }
}
