package com.rarible.protocol.order.core.service.auction

import com.rarible.core.common.optimisticLock
import com.rarible.core.reduce.service.UpdateService
import com.rarible.protocol.order.core.event.AuctionListener
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.AuctionOffchainHistory
import com.rarible.protocol.order.core.model.AuctionStatus
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import org.springframework.stereotype.Component

@Component
class AuctionUpdateService(
    private val auctionRepository: AuctionRepository,
    private val auctionListeners: List<AuctionListener>,
    private val auctionStateService: AuctionStateService
) : UpdateService<Auction> {


    override suspend fun update(data: Auction) {
        if (data.deleted.not()) {
            val updatedAuction = optimisticLock {
                val currentAuction = auctionRepository.findById(data.hash)
                val newVersion = currentAuction?.let {
                    data.copy(
                        version = currentAuction.version,
                        // This field is managed by indexer, so reducer should not change it
                        ongoing = currentAuction.ongoing
                    )
                } ?: data
                // We need to call here withCalculatedState() again in order to perform
                // force reset of 'ongoing' if state of Auction is not ACTIVE anymore
                val updated = auctionRepository.save(newVersion.withCalculatedState())
                if (currentAuction != null) {
                    // Means we got FINISHED event from chain before status job recalculated 'ongoing' field
                    // In such case we need to send ENDED activity event before FINISHED
                    if (currentAuction.ongoing && data.status == AuctionStatus.FINISHED) {
                        auctionStateService.onAuctionOngoingStateUpdated(updated, AuctionOffchainHistory.Type.ENDED)
                    }
                }
                updated
            }
            auctionListeners.forEach { listener -> listener.onAuctionUpdate(updatedAuction) }
        } else {
            auctionRepository.remove(data.hash)
            auctionListeners.forEach { listener -> listener.onAuctionDelete(data) }
        }
    }
}
