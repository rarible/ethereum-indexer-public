package com.rarible.protocol.order.core.model

import com.rarible.protocol.contracts.exchange.crypto.punks.PunkBidEnteredEvent
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkBidWithdrawnEvent
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkBoughtEvent
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkNoLongerForSaleEvent
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkOfferedEvent
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkTransferEvent
import com.rarible.protocol.contracts.exchange.looksrare.v1.CancelMultipleOrdersEvent
import com.rarible.protocol.contracts.exchange.looksrare.v1.TakerAskEvent
import com.rarible.protocol.contracts.exchange.looksrare.v1.TakerBidEvent
import com.rarible.protocol.contracts.exchange.v2.events.MatchEventDeprecated
import com.rarible.protocol.contracts.exchange.v2.events.UpsertOrderEvent
import com.rarible.protocol.contracts.exchange.v2.rev3.MatchEvent
import com.rarible.protocol.contracts.exchange.x2y2.v1.EvCancelEvent
import com.rarible.protocol.contracts.exchange.zero.ex.FillEvent
import com.rarible.protocol.contracts.seaport.v1.events.OrderFulfilledEvent
import com.rarible.protocol.contracts.x2y2.v1.events.EvInventoryEvent
import io.daonomic.rpc.domain.Word
import com.rarible.protocol.contracts.blur.v1.evemts.OrdersMatchedEvent as BlurOrdersMatchedEvent
import com.rarible.protocol.contracts.exchange.blur.v1.OrderCancelledEvent as BlurOrderCancelledEvent
import com.rarible.protocol.contracts.exchange.seaport.v1.OrderCancelledEvent as SeaportOrderCancelledEvent
import com.rarible.protocol.contracts.exchange.v2.events.MatchEvent as MatchEventLegacy

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
            MatchEventLegacy.id(),
            MatchEventDeprecated.id(),
            // OrdersMatchedEvent.id(), //TODO: Activate after move to a new scanner
            FillEvent.id(),
            OrderFulfilledEvent.id(),
            PunkBoughtEvent.id(),
            TakerAskEvent.id(),
            TakerBidEvent.id(),
            EvInventoryEvent.id(),
            BlurOrdersMatchedEvent.id()
        )
    ),
    CANCEL(
        topic = setOf(
            com.rarible.protocol.contracts.exchange.v1.CancelEvent.id(),
            com.rarible.protocol.contracts.exchange.v2.events.CancelEvent.id(),
            com.rarible.protocol.contracts.exchange.v2.events.CancelEventDeprecated.id(),
            // OrderCancelledEvent.id(), //TODO: Activate after move to a new scanner
            PunkNoLongerForSaleEvent.id(),
            PunkBidWithdrawnEvent.id(),
            PunkTransferEvent.id(),
            SeaportOrderCancelledEvent.id(),
            CancelMultipleOrdersEvent.id(),
            EvCancelEvent.id(),
            BlurOrderCancelledEvent.id()
        )
    ),
}
