package com.rarible.protocol.order.core.model

import com.rarible.protocol.contracts.auction.v1.event.AuctionCreatedEvent
import com.rarible.protocol.contracts.exchange.v2.events.UpsertOrderEvent
import io.daonomic.rpc.domain.Word

enum class AuctionHistoryType(val topic: Set<Word>) {
    ON_CHAIN_AUCTION(
        topic = setOf(
            AuctionCreatedEvent.id()
        )
    ),
    BID_PLACED(
        topic = setOf(
            UpsertOrderEvent.id()
        )
    ),
    AUCTION_FINISHED(
        topic = setOf(
            UpsertOrderEvent.id()
        )
    ),
    AUCTION_CANCELLED(
        topic = setOf(
            UpsertOrderEvent.id()
        )
    )
}
