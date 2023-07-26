package com.rarible.protocol.order.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.ethereum.domain.EthUInt256
import org.springframework.data.annotation.Transient
import scala.Tuple3
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "version")
@JsonSubTypes(
    JsonSubTypes.Type(value = BidV1::class, name = "RARIBLE_AUCTION_V1_DATA_V1"),
)
sealed class Bid {
    abstract val amount: EthUInt256
    abstract val data: BidData
    abstract val date: Instant
    abstract val version: AuctionBidVersion

    fun forTx() = Tuple3(
        amount.value,
        data.toDataVersion(),
        data.toEthereum().bytes()
    )
}

data class BidV1(
    override val amount: EthUInt256,
    override val data: BidDataV1,
    override val date: Instant
) : Bid() {
    @get:Transient
    override val version: AuctionBidVersion
        get() = AuctionBidVersion.RARIBLE_AUCTION_V1_DATA_V1
}

enum class AuctionBidVersion {
    RARIBLE_AUCTION_V1_DATA_V1,
}
