package com.rarible.protocol.order.core.model

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import scalether.domain.Address
import java.math.BigInteger

sealed class OrderFilter {
    abstract val origin: Address?
    abstract val platforms: List<PlatformDto>
    abstract val sort: Sort
    abstract val status: List<OrderStatusDto>?
    abstract val currency: Address?

    enum class Sort {
        LAST_UPDATE_DESC,
        LAST_UPDATE_ASC,
        TAKE_PRICE_DESC,
        MAKE_PRICE_ASC
    }

}

data class OrderFilterAll(
    override val origin: Address? = null,
    override val platforms: List<PlatformDto>,
    override val sort: Sort,
    override val status: List<OrderStatusDto>? = null,
    override val currency: Address? = null
) : OrderFilter()

data class OrderFilterSell(
    override val origin: Address? = null,
    override val platforms: List<PlatformDto>,
    override val sort: Sort,
    override val status: List<OrderStatusDto>? = null,
    override val currency: Address? = null
) : OrderFilter()

data class OrderFilterSellByItem(
    override val origin: Address? = null,
    override val platforms: List<PlatformDto>,
    override val sort: Sort,
    override val status: List<OrderStatusDto>? = null,
    override val currency: Address? = null,
    val contract: Address,
    val tokenId: BigInteger,
    val maker: Address? = null
) : OrderFilter()

data class OrderFilterSellByCollection(
    override val origin: Address? = null,
    override val platforms: List<PlatformDto>,
    override val sort: Sort,
    override val status: List<OrderStatusDto>? = null,
    override val currency: Address? = null,
    val collection: Address
) : OrderFilter()

data class OrderFilterSellByMaker(
    override val origin: Address? = null,
    override val platforms: List<PlatformDto>,
    override val sort: Sort,
    override val status: List<OrderStatusDto>? = null,
    override val currency: Address? = null,
    val maker: Address
) : OrderFilter()

data class OrderFilterBidByItem(
    override val origin: Address? = null,
    override val platforms: List<PlatformDto>,
    override val sort: Sort,
    override val status: List<OrderStatusDto>? = null,
    override val currency: Address? = null,
    val contract: Address,
    val tokenId: BigInteger,
    val maker: Address? = null
) : OrderFilter()

data class OrderFilterBidByMaker(
    override val origin: Address? = null,
    override val platforms: List<PlatformDto>,
    override val sort: Sort,
    override val status: List<OrderStatusDto>? = null,
    override val currency: Address? = null,
    val maker: Address
) : OrderFilter()
