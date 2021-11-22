package com.rarible.protocol.order.core.model

data class AuctionBids(
    val bidPlaced: List<BidPlaced>,
    val auction: Auction
)
