package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant

sealed class AuctionHistory(
    var type: AuctionHistoryType
) : EventData {
    abstract val hash: Word
    abstract val date: Instant
    abstract val contract: Address
    abstract val source: HistorySource
}

data class OnChainAuction(
    val auctionType: AuctionType,
    override val seller: Address,
    override val buyer: Address?,
    override val sell: Asset,
    override val buy: AssetType,
    override val lastBid: Bid?,
    override val endTime: Instant?,
    override val minimalStep: EthUInt256,
    override val minimalPrice: EthUInt256,
    override val data: AuctionData,
    override val protocolFee: EthUInt256,
    val createdAt: Instant,
    val auctionId: EthUInt256,
    override val hash: Word,
    override val contract: Address,
    override val date: Instant,
    override val source: HistorySource
): BaseAuction, AuctionHistory(AuctionHistoryType.ON_CHAIN_AUCTION)

data class BidPlaced(
    val bid: Bid,
    val buyer: Address,
    val endTime: EthUInt256,
    val auctionId: EthUInt256,
    val bidValue: BigDecimal?,
    override val hash: Word,
    override val contract: Address,
    override val date: Instant,
    override val source: HistorySource
) : AuctionHistory(AuctionHistoryType.BID_PLACED)

data class AuctionFinished(
    override val seller: Address,
    override val buyer: Address?,
    override val sell: Asset,
    override val buy: AssetType,
    override val lastBid: Bid?,
    override val endTime: Instant?,
    override val minimalStep: EthUInt256,
    override val minimalPrice: EthUInt256,
    override val data: AuctionData,
    override val protocolFee: EthUInt256,
    val createdAt: Instant,
    val auctionId: EthUInt256,
    override val hash: Word,
    override val contract: Address,
    override val date: Instant,
    override val source: HistorySource
) : BaseAuction, AuctionHistory(AuctionHistoryType.AUCTION_FINISHED)

data class AuctionCancelled(
    val auctionId: EthUInt256,
    override val hash: Word,
    override val contract: Address,
    override val date: Instant,
    override val source: HistorySource
) : AuctionHistory(AuctionHistoryType.AUCTION_CANCELLED)
