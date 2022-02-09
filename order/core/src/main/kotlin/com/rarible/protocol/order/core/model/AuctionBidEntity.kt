package com.rarible.protocol.order.core.model

import scalether.domain.Address
import java.time.Instant

data class AuctionBidEntity(
    val id: String,
    val bid: Bid,
    val buy: AssetType,
    val buyer: Address,
    val date: Instant,
    val status: Status
) {

    enum class Status {
        ACTIVE, HISTORICAL
    }
}
