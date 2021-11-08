package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.time.Instant

sealed class AuctionHistory(
    val type: AuctionHistoryType
) : EventData {
    abstract val hash: Word
    abstract val date: Instant
    abstract val contract: Address
}

data class OnChainAuction(
    val auctionType: AuctionType,
    val seller: Address,
    val buyer: Address?,
    val sell: Asset,
    val buy: AssetType,
    val lastBid: Bid?,
    val startTime: Instant,
    val endTime: Instant,
    val minimalStep: EthUInt256,
    val minimalPrice: EthUInt256,
    val data: AuctionData,
    val createdAt: Instant,
    val auctionId: EthUInt256,
    val protocolFee: EthUInt256,
    override val hash: Word,
    override val contract: Address,
    override val date: Instant = createdAt
): AuctionHistory(AuctionHistoryType.ON_CHAIN_AUCTION)

data class BidPlaced(
    val bid: BidV1,
    val endTime: Instant,
    val auctionId: EthUInt256,
    override val hash: Word,
    override val contract: Address,
    override val date: Instant
) : AuctionHistory(AuctionHistoryType.BID_PLACED)

data class AuctionFinished(
    val auctionId: EthUInt256,
    override val hash: Word,
    override val contract: Address,
    override val date: Instant
) : AuctionHistory(AuctionHistoryType.AUCTION_FINISHED)


data class AuctionCancelled(
    val auctionId: EthUInt256,
    override val hash: Word,
    override val contract: Address,
    override val date: Instant
) : AuctionHistory(AuctionHistoryType.AUCTION_CANCELLED)
