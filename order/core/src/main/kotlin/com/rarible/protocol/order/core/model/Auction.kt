package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import io.daonomic.rpc.domain.Word
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.mapping.Document
import scala.Tuple2
import scalether.domain.Address
import java.time.Instant

@Document("auction")
data class Auction(
    val type: AuctionType,
    val seller: Address,
    val buyer: Address?,
    val sell: Asset,
    val buy: AssetType,
    val lastBid: Bid?,
    val startTime: Instant,
    val endTime: Instant,
    val minimalStep: EthUInt256,
    val minimalPrice: EthUInt256,
    val canceled: Boolean,
    val data: AuctionData,
    val createdAt: Instant,
    val lastUpdatedAy: Instant,
    val auctionId: EthUInt256,
    val protocolFee: EthUInt256,
    val contract: Address,
    val pending: List<AuctionHistory>
) {
    @Transient
    private val _id: Word = hashKey(this)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var hash: Word
        get() = _id
        set(_) {}

    companion object {
        fun hashKey(auction: Auction): Word {
            return when (auction.type) {
                AuctionType.RARIBLE_V1 -> Tuples.keccak256(
                    Tuples.raribleAuctionKeyHashType().encode(
                        Tuple2(
                            auction.contract,
                            auction.auctionId.value
                        )
                    )
                )
            }
        }
    }
}
