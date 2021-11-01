package com.rarible.protocol.order.core.event

import com.rarible.protocol.order.core.model.Auction
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component

@Component
class AuctionListener {
    suspend fun onAuctionUpdate(auction: Auction) {
    }

    suspend fun onAuctionDelete(hash: Word) {
    }
}
