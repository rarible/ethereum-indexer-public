package com.rarible.protocol.order.core.model

import com.rarible.protocol.contracts.exchange.crypto.punks.*
import com.rarible.protocol.contracts.exchange.v2.events.MatchEvent
import com.rarible.protocol.contracts.exchange.v2.events.MatchEventDeprecated
import com.rarible.protocol.contracts.exchange.v2.events.UpsertOrderEvent
import com.rarible.protocol.contracts.exchange.wyvern.OrderCancelledEvent
import com.rarible.protocol.contracts.exchange.wyvern.OrdersMatchedEvent
import io.daonomic.rpc.domain.Word

enum class ItemType(
    val topic: Set<Word>
) {
    BUY(
        topic = setOf(
            com.rarible.protocol.contracts.exchange.v1.BuyEvent.id()
        )
    ),
    ON_CHAIN_ORDER(
        topic = setOf(
            UpsertOrderEvent.id(),
            PunkOfferedEvent.id(),
            PunkBidEnteredEvent.id()
        )
    ),
    ORDER_SIDE_MATCH(
        topic = setOf(
            MatchEvent.id(),
            MatchEventDeprecated.id(),
            OrdersMatchedEvent.id(),
            PunkBoughtEvent.id()
        )
    ),
    CANCEL(
        topic= setOf(
            com.rarible.protocol.contracts.exchange.v1.CancelEvent.id(),
            com.rarible.protocol.contracts.exchange.v2.events.CancelEvent.id(),
            com.rarible.protocol.contracts.exchange.v2.events.CancelEventDeprecated.id(),
            OrderCancelledEvent.id(),
            PunkNoLongerForSaleEvent.id(),
            PunkBidWithdrawnEvent.id()
        )
    ),
}
