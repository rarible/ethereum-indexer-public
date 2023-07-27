package com.rarible.protocol.order.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.blockchain.scanner.ethereum.model.EventData
import com.rarible.ethereum.domain.EthUInt256
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = OnChainAuction::class, name = "ON_CHAIN_AUCTION"),
    JsonSubTypes.Type(value = BidPlaced::class, name = "BID_PLACED"),
    JsonSubTypes.Type(value = AuctionFinished::class, name = "AUCTION_FINISHED"),
    JsonSubTypes.Type(value = AuctionCancelled::class, name = "AUCTION_CANCELLED"),
)
sealed class AuctionHistory(
    var type: AuctionHistoryType
) : EventData {
    abstract val hash: Word
    abstract val date: Instant
    abstract val contract: Address
    abstract val source: HistorySource

    override fun getKey(log: EthereumLog): String {
        return hash.prefixed()
    }
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
) : BaseAuction, AuctionHistory(AuctionHistoryType.ON_CHAIN_AUCTION)

data class BidPlaced(
    val bid: Bid,
    val buyer: Address,
    val endTime: EthUInt256,
    val auctionId: EthUInt256,
    val sell: Asset?,
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
    val seller: Address?,
    val sell: Asset?,
    override val hash: Word,
    override val contract: Address,
    override val date: Instant,
    override val source: HistorySource
) : AuctionHistory(AuctionHistoryType.AUCTION_CANCELLED)
