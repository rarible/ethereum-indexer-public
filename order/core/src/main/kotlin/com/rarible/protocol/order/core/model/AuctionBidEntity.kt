package com.rarible.protocol.order.core.model

import scalether.domain.Address

data class AuctionBidEntity(
    val id: String,
    val bid: Bid,
    val buy: AssetType,
    val buyer: Address
)
