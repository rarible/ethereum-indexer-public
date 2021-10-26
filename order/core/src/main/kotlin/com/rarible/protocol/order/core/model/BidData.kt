package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import org.springframework.data.annotation.Transient

sealed class BidData {
    abstract val version: BidDataVersion
}

data class BidDataV1(
    val originFees: List<Part>
) : BidData() {

    @get:Transient
    override val version: BidDataVersion
        get() = BidDataVersion.RARIBLE_AUCTION_BID_V1_DATA_V1
}

enum class BidDataVersion(val ethDataType: Binary) {
    RARIBLE_AUCTION_BID_V1_DATA_V1(id("V1")),
}
