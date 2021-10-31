package com.rarible.protocol.order.core.model

import com.rarible.protocol.contracts.auction.v1.event.AuctionCancelledEvent
import com.rarible.protocol.contracts.auction.v1.event.AuctionCreatedEvent
import com.rarible.protocol.contracts.auction.v1.event.AuctionFinishedEvent
import com.rarible.protocol.contracts.auction.v1.event.BidPlacedEvent
import io.daonomic.rpc.domain.Word

enum class AuctionHistoryType(val topic: Set<Word>) {
    ON_CHAIN_AUCTION(
        topic = setOf(
            AuctionCreatedEvent.id()
        )
    ),
    BID_PLACED(
        topic = setOf(
            BidPlacedEvent.id()
        )
    ),
    AUCTION_FINISHED(
        topic = setOf(
            AuctionFinishedEvent.id()
        )
    ),
    AUCTION_CANCELLED(
        topic = setOf(
            AuctionCancelledEvent.id()
        )
    )
}
