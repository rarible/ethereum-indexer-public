package com.rarible.protocol.order.core.model

import com.rarible.protocol.contracts.exchange.v2.events.UpsertOrderEvent
import io.daonomic.rpc.domain.Word

enum class AuctionHistoryType(val topic: Set<Word>) {
    ON_CHAIN_AUCTION(
        topic = setOf(
            UpsertOrderEvent.id()
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
