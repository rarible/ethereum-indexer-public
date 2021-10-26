package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import io.daonomic.rpc.domain.Binary
import org.springframework.data.annotation.Transient
import java.time.Duration
import java.time.Instant

sealed class AuctionData {
    abstract val version: AuctionDataVersion
}

data class AuctionDataV1(
    val originFees: List<Part>,
    val duration: Duration,
    val startTime: Instant,
    val buyOutPrice: EthUInt256
) : AuctionData() {

    @get:Transient
    override val version: AuctionDataVersion
        get() = AuctionDataVersion.RARIBLE_AUCTION_V1_DATA_V1
}

enum class AuctionDataVersion(val ethDataType: Binary) {
    RARIBLE_AUCTION_V1_DATA_V1(id("V1")),
}
