package com.rarible.protocol.order.core.model

import com.rarible.protocol.contracts.exchange.crypto.punks.PunkBidEnteredEvent
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkBidWithdrawnEvent
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkBoughtEvent
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkNoLongerForSaleEvent
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkOfferedEvent
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkTransferEvent
import com.rarible.protocol.contracts.exchange.looksrare.v1.CancelAllOrdersEvent
import com.rarible.protocol.contracts.exchange.looksrare.v1.CancelMultipleOrdersEvent
import com.rarible.protocol.contracts.exchange.looksrare.v1.TakerAskEvent
import com.rarible.protocol.contracts.exchange.looksrare.v1.TakerBidEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.NFTDepositEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.NewPairEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.DeltaUpdateEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.NFTWithdrawalEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SpotPriceUpdateEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SwapNFTInPairEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SwapNFTOutPairEvent
import com.rarible.protocol.contracts.exchange.v2.events.MatchEvent as MatchEventLegacy
import com.rarible.protocol.contracts.exchange.v2.rev3.MatchEvent
import com.rarible.protocol.contracts.exchange.v2.events.MatchEventDeprecated
import com.rarible.protocol.contracts.exchange.v2.events.UpsertOrderEvent
import com.rarible.protocol.contracts.exchange.wyvern.OrderCancelledEvent
import com.rarible.protocol.contracts.exchange.seaport.v1.OrderCancelledEvent as SeaportOrderCancelledEvent
import com.rarible.protocol.contracts.exchange.wyvern.OrdersMatchedEvent
import com.rarible.protocol.contracts.exchange.x2y2.v1.EvCancelEvent
import com.rarible.protocol.contracts.exchange.zero.ex.FillEvent
import com.rarible.protocol.contracts.seaport.v1.events.OrderFulfilledEvent
import com.rarible.protocol.contracts.x2y2.v1.events.EvInventoryEvent
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
    AMM_ORDER(
        topic = setOf(
            NewPairEvent.id(),
        )
    ),
    POOL_NFT_OUT(
        topic = setOf(
            SwapNFTOutPairEvent.id(),
        )
    ),
    POOL_NFT_IN(
        topic = setOf(
            SwapNFTInPairEvent.id(),
        )
    ),
    POOL_NFT_WITHDRAW(
        topic = setOf(
            NFTWithdrawalEvent.id(),
        )
    ),
    POOL_NFT_DEPOSIT(
        topic = setOf(
            NFTDepositEvent.id(),
        )
    ),
    POOL_SPOT_PRICE_UPDATE(
        topic = setOf(
            SpotPriceUpdateEvent.id(),
        )
    ),
    POOL_DELTA_UPDATE(
        topic = setOf(
            DeltaUpdateEvent.id(),
        )
    ),
    ORDER_SIDE_MATCH(
        topic = setOf(
            MatchEvent.id(),
            MatchEventLegacy.id(),
            MatchEventDeprecated.id(),
            OrdersMatchedEvent.id(),
            FillEvent.id(),
            OrderFulfilledEvent.id(),
            PunkBoughtEvent.id(),
            TakerAskEvent.id(),
            TakerBidEvent.id(),
            EvInventoryEvent.id()
        )
    ),
    CANCEL(
        topic= setOf(
            com.rarible.protocol.contracts.exchange.v1.CancelEvent.id(),
            com.rarible.protocol.contracts.exchange.v2.events.CancelEvent.id(),
            com.rarible.protocol.contracts.exchange.v2.events.CancelEventDeprecated.id(),
            OrderCancelledEvent.id(),
            PunkNoLongerForSaleEvent.id(),
            PunkBidWithdrawnEvent.id(),
            PunkTransferEvent.id(),
            SeaportOrderCancelledEvent.id(),
            CancelMultipleOrdersEvent.id(),
            EvCancelEvent.id()
        )
    ),
}
