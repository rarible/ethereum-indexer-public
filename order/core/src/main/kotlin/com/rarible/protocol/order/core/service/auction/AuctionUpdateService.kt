package com.rarible.protocol.order.core.service.auction

import com.rarible.protocol.order.core.event.AuctionListener
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component

/**
 * Service responsible for inserting or updating auction state (see [save]).
 */
@Component
class AuctionUpdateService(
    private val auctionRepository: AuctionRepository,
    private val auctionReduceService: AuctionReduceService,
    private val auctionListener: AuctionListener
) {
    suspend fun update(hash: Word) {
        val auction = auctionRepository.findById(hash)
        val updatedAuction = auctionReduceService.updateAuction(hash)

        if (updatedAuction != null && auction?.lastEventId != updatedAuction.lastEventId) {
            auctionListener.onAuctionUpdate(updatedAuction)
        }
    }
}
