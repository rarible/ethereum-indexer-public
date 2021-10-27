package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.time.Instant

sealed class AuctionHistory(var type: AuctionHistoryType) : EventData {
    abstract val hash: Word
    abstract val date: Instant
}

sealed class OnChainAuction : AuctionHistory(AuctionHistoryType.ON_CHAIN_AUCTION) {
    abstract val seller: Address
    abstract val buyer: Address?
    abstract val sell: Asset
    abstract val buy: AssetType
    abstract val lastBid: Bid
    abstract val startTime: Instant
    abstract val endTime: Instant
    abstract val minimalStep: EthUInt256
    abstract val minimalPrice: EthUInt256
    abstract val data: AuctionData
    abstract val createdAt: Instant

    data class OnChainRaribleAuctionV1(
        override val hash: Word,
        val auctionId: EthUInt256,
        override val seller: Address,
        override val buyer: Address?,
        override val sell: Asset,
        override val buy: AssetType,
        override val lastBid: BidV1,
        override val startTime: Instant,
        override val endTime: Instant,
        override val minimalStep: EthUInt256,
        override val minimalPrice: EthUInt256,
        override val data: AuctionDataV1,
        override val createdAt: Instant,
        override val date: Instant = createdAt,
        val protocolFee: EthUInt256,
        val auction: Address
    ) : OnChainAuction()
}

sealed class BidPlaced : AuctionHistory(AuctionHistoryType.BID_PLACED) {
    abstract val bid: Bid

    data class BidPlacedRaribleV1(
        override val hash: Word,
        val auctionId: EthUInt256,
        val auction: Address,
        override val bid: BidV1,
        val endTime: Instant,
        override val date: Instant
    ) : BidPlaced()
}

sealed class AuctionFinished : AuctionHistory(AuctionHistoryType.AUCTION_FINISHED) {

    data class AuctionFinishedRaribleV1(
        override val hash: Word,
        val auctionId: EthUInt256,
        val auction: Address,
        override val date: Instant
    ) : AuctionFinished()
}


sealed class AuctionBuyOut : AuctionHistory(AuctionHistoryType.AUCTION_BUY_OUT) {

    data class AuctionBuyOutRaribleV1(
        override val hash: Word,
        val auctionId: EthUInt256,
        val auction: Address,
        override val date: Instant
    ) : AuctionBuyOut()
}

sealed class AuctionCancelled : AuctionHistory(AuctionHistoryType.AUCTION_CANCELLED) {

    data class AuctionCancelledRaribleV1(
        override val hash: Word,
        val auctionId: EthUInt256,
        val auction: Address,
        override val date: Instant
    ) : AuctionCancelled()
}
