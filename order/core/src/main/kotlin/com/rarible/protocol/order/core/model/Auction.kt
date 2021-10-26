package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address
import java.time.Instant

sealed class Auction {
    abstract val seller: Address
    abstract val buyer: Address?
    abstract val sell: Asset
    abstract val buy: AssetType
    abstract val lastBid: Bid
    abstract val startTime: Instant
    abstract val endTime: Instant
    abstract val minimalStep: EthUInt256
    abstract val minimalPrice: EthUInt256
    abstract val protocolFee: EthUInt256
    abstract val data: AuctionData
}

data class RaribleAuctionV1(
    override val seller: Address,
    override val buyer: Address?,
    override val sell: Asset,
    override val buy: AssetType,
    override val lastBid: BidV1,
    override val startTime: Instant,
    override val endTime: Instant,
    override val minimalStep: EthUInt256,
    override val minimalPrice: EthUInt256,
    override val protocolFee: EthUInt256,
    override val data: AuctionDataV1
) : Auction()
