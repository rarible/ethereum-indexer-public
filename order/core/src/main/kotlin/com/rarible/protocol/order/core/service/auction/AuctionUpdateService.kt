package com.rarible.protocol.order.core.service.auction

import com.rarible.core.reduce.service.UpdateService
import com.rarible.protocol.order.core.event.AuctionListener
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import org.springframework.stereotype.Component

@Component
class AuctionUpdateService(
    private val auctionRepository: AuctionRepository,
    private val auctionListeners: List<AuctionListener>
) : UpdateService<Auction> {

    override suspend fun update(data: Auction) {
        if (data.deleted.not()) {
            val updatedAuction = auctionRepository.save(data.withCalculatedState())
            auctionListeners.forEach { listener -> listener.onAuctionUpdate(updatedAuction) }
        } else {
            auctionRepository.remove(data.hash)
            auctionListeners.forEach { listener -> listener.onAuctionDelete(data.hash) }
        }
    }
}
