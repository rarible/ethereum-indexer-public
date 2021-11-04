package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import io.daonomic.rpc.domain.Word
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.mapping.Document
import scala.Tuple2
import scala.Tuple6
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant

@Document("auction")
data class Auction(
    val type: AuctionType,
    val status: AuctionStatus,
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
    val finished: Boolean,
    val cancelled: Boolean,
    val createdAt: Instant,
    val lastUpdateAt: Instant,
    val lastEventId: String?,
    val auctionId: EthUInt256,
    val contract: Address,
    val pending: List<AuctionHistory>,
    val buyPrice: BigDecimal?,
    val buyPriceUsd: BigDecimal?,
    val platform: Platform
) : BaseAuction {

    @Transient
    private val _id: Word = hashKey(this)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var hash: Word
        get() = _id
        set(_) {}

    fun forTx() = Tuple6(
        sell.forTx(),
        buy.forTx(),
        minimalStep.value,
        minimalPrice.value,
        data.getDataVersion(),
        data.toEthereum().bytes()
    )

    fun withCalculatedSate(): Auction {
        return when {
            cancelled -> copy(status = AuctionStatus.CANCELLED)
            finished -> copy(status = AuctionStatus.FINISHED)
            else -> copy(status = AuctionStatus.ACTIVE)
        }
    }

    companion object {
        fun hashKey(auction: Auction): Word {
            return when (auction.type) {
                AuctionType.RARIBLE_V1 -> raribleV1HashKey(auction.contract, auction.auctionId)
            }
        }

        fun raribleV1HashKey(auction: Address, auctionId: EthUInt256): Word {
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
