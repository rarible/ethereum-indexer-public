package com.rarible.protocol.order.core.repository.auction

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.AuctionStatus
import com.rarible.protocol.order.core.model.Platform
import scalether.domain.Address

sealed class AuctionFilter {
    abstract val sort: AuctionSort
    abstract val origin: Address?
    abstract val status: List<AuctionStatus>?
    abstract val platform: Platform?
    abstract val currency: Address?

    data class All(
        override val sort: AuctionSort,
        override val origin: Address?,
        override val status: List<AuctionStatus>?,
        override val platform: Platform?,
        override val currency: Address?
    ) : AuctionFilter()

    data class BySeller(
        val seller: Address,
        override val sort: AuctionSort,
        override val origin: Address?,
        override val status: List<AuctionStatus>?,
        override val platform: Platform?,
        override val currency: Address?
    ) : AuctionFilter()

    data class ByItem(
        val token: Address,
        val tokenId: EthUInt256,
        val seller: Address?,
        override val sort: AuctionSort,
        override val origin: Address?,
        override val status: List<AuctionStatus>?,
        override val platform: Platform?,
        override val currency: Address?
    ) : AuctionFilter()

    data class ByCollection(
        val token: Address,
        val seller: Address?,
        override val sort: AuctionSort,
        override val origin: Address?,
        override val status: List<AuctionStatus>?,
        override val platform: Platform?,
        override val currency: Address?
    ) : AuctionFilter()

    enum class AuctionSort {
        LAST_UPDATE_ASC,
        LAST_UPDATE_DESC,
        BUY_PRICE_ASC
    }
}
