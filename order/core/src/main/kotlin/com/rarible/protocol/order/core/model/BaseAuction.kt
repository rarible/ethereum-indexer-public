package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address
import java.time.Instant

interface BaseAuction {
    val seller: Address
    val buyer: Address?
    val sell: Asset
    val buy: AssetType
    val lastBid: Bid?
    val endTime: Instant?
    val minimalStep: EthUInt256
    val minimalPrice: EthUInt256
    val data: AuctionData
    val protocolFee: EthUInt256
}
