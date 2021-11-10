package com.rarible.protocol.order.core.model

import com.rarible.protocol.contracts.Tuples
import io.daonomic.rpc.domain.Binary
import org.springframework.data.annotation.Transient
import scala.Tuple2

sealed class BidData {
    abstract val version: BidDataVersion

    fun getDataVersion(): ByteArray = version.ethDataType.bytes()

    abstract fun toEthereum(): Binary

    companion object {
        fun decode(version: Binary, data: Binary): BidData {
            return when (version) {
                BidDataVersion.RARIBLE_AUCTION_BID_V1_DATA_V1.ethDataType -> {
                    val decoded = Tuples.auctionBidDataV1Type().decode(data, 0)
                    BidDataV1(
                        payouts = decoded.value()._1().map { it.toPart() },
                        originFees = decoded.value()._2().map { it.toPart() }
                    )
                }
                else -> throw IllegalArgumentException("Can't parse auction bid data")
            }
        }
    }
}

data class BidDataV1(
    val payouts: List<Part>,
    val originFees: List<Part>
) : BidData() {

    @get:Transient
    override val version: BidDataVersion
        get() = BidDataVersion.RARIBLE_AUCTION_BID_V1_DATA_V1

    override fun toEthereum(): Binary {
        return Tuples.auctionBidDataV1Type().encode(
            Tuple2(
                payouts.map { it.toEthereum() }.toTypedArray(),
                originFees.map { it.toEthereum() }.toTypedArray()
            )
        )
    }
}

enum class BidDataVersion(val ethDataType: Binary) {
    RARIBLE_AUCTION_BID_V1_DATA_V1(id("V1")),
}
