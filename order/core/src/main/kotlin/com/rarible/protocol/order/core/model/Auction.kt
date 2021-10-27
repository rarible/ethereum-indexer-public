package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import io.daonomic.rpc.domain.Word
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import scala.Tuple2
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
    abstract val canceled: Boolean
    abstract val data: AuctionData
    abstract val createdAt: Instant
    abstract val lastUpdatedAy: Instant
}

data class RaribleAuctionV1(
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
    override val canceled: Boolean,
    override val data: AuctionDataV1,
    override val createdAt: Instant,
    override val lastUpdatedAy: Instant,
    val protocolFee: EthUInt256,
    val auction: Address
) : Auction() {

    @Transient
    private val _id: Word = hashKey(auction, auctionId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: Word
        get() = _id
        set(_) {}

    companion object {
        fun hashKey(auction: Address, auctionId: EthUInt256): Word {
            return Tuples.keccak256(
                Tuples.raribleAuctionKeyHashType().encode(
                    Tuple2(
                        auction,
                        auctionId.value
                    )
                )
            )
        }
    }
}
