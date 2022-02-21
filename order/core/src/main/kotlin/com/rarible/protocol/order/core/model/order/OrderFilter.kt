package com.rarible.protocol.order.core.model.order

import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.converters.model.PlatformConverter
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.token
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import scalether.domain.Address
import java.math.BigDecimal

val logger: Logger = LoggerFactory.getLogger(OrderFilter::class.java)

sealed class OrderFilter {

    abstract val origin: Address?
    abstract val platforms: List<PlatformDto>
    abstract val sort: OrderFilterSort
    abstract val status: List<OrderStatusDto>?

    val hasPlatforms get() = platforms.isNotEmpty()
    val hasStatuses get() = !status.isNullOrEmpty()

    abstract fun toQuery(continuation: String?, limit: Int): Query

    open fun toContinuation(order: Order) = when (sort) {
        OrderFilterSort.LAST_UPDATE_DESC -> {
            Continuation.LastDate(order.lastUpdateAt, order.hash)
        }
        OrderFilterSort.LAST_UPDATE_ASC -> {
            Continuation.LastDate(order.lastUpdateAt, order.hash)
        }
        OrderFilterSort.TAKE_PRICE_DESC -> {
            order.takePriceUsd?.let {
                logger.warn("Using deprecated field ${Order::takePriceUsd.name} for sorting")
            }
            Continuation.Price(order.takePriceUsd ?: BigDecimal.ZERO, order.hash)
        }
        OrderFilterSort.MAKE_PRICE_ASC -> {
            order.makePriceUsd?.let {
                logger.warn("Using deprecated field ${Order::makePriceUsd.name} for sorting")
            }
            Continuation.Price(order.makePriceUsd ?: BigDecimal.ZERO, order.hash)
        }
    }.toString()

    protected fun sort(sort: OrderFilterSort): Sort {
        return when (sort) {
            OrderFilterSort.LAST_UPDATE_ASC -> Sort.by(
                Sort.Direction.ASC,
                Order::lastUpdateAt.name,
                Order::hash.name
            )
            else -> Sort.by(
                Sort.Direction.DESC,
                Order::lastUpdateAt.name,
                Order::hash.name
            )
        }
    }

    protected fun sort(sort: OrderFilterSort, currency: Address?): Sort {
        return when (sort) {
            OrderFilterSort.MAKE_PRICE_ASC -> {
                currency ?: logger.warn("Using deprecated field ${Order::makePriceUsd.name} for sorting")
                Sort.by(
                    Sort.Direction.ASC,
                    (currency?.let { Order::makePrice } ?: Order::makePriceUsd).name,
                    Order::hash.name
                )
            }
            OrderFilterSort.TAKE_PRICE_DESC -> {
                currency ?: logger.warn("Using deprecated field ${Order::takePriceUsd} for sorting")
                Sort.by(
                    Sort.Direction.DESC,
                    (currency?.let { Order::takePrice } ?: Order::takePriceUsd).name,
                    Order::hash.name
                )
            }
            else -> sort(sort)
        }
    }

    protected fun convert(platform: PlatformDto): Platform? = PlatformConverter.convert(platform)
}

enum class OrderFilterSort {
    LAST_UPDATE_DESC,
    LAST_UPDATE_ASC,
    TAKE_PRICE_DESC,
    MAKE_PRICE_ASC
}

fun Criteria.forCurrency(currency: Address?): Criteria {
    return currency?.let {
        if (it.equals(Address.ZERO())) { // zero means ETH
            and(Order::take / Asset::type / AssetType::token).exists(false)
        } else {
            and(Order::take / Asset::type / AssetType::token).isEqualTo(Address.apply(it))
        }
    } ?: this
}

fun Criteria.fromOrigin(origin: Address?) = origin?.let {
    and(Order::data / OrderRaribleV2DataV1::originFees)
        .elemMatch(Criteria.where(Part::account.name).`is`(origin))
} ?: this

fun Criteria.forStatus(status: List<OrderStatusDto>?) =
    if (status?.isNotEmpty() == true) {
        var statuses = status.map { OrderStatus.valueOf(it.name) }.toMutableList()
        if (OrderStatus.INACTIVE in statuses) {
            statuses += listOf(OrderStatus.ENDED, OrderStatus.NOT_STARTED)
        }
        and(Order::status).inValues(statuses)
    } else this

fun Criteria.forPlatform(platforms: List<Platform>): Criteria {
    return if (platforms.isEmpty()) {
        this
    } else {
        and(Order::platform).inValues(platforms)
    }
}

fun Criteria.forMaker(maker: Address?) = maker?.let { and(Order::maker).isEqualTo(it) } ?: this

fun Criteria.forMakers(maker: List<Address>?): Criteria {
    return if (maker.isNullOrEmpty()) this else and(Order::maker).inValues(maker)
}

fun Criteria.forCollection(collection: Address) =
    and(Order::make / Asset::type / NftAssetType::token).isEqualTo(collection)

fun Criteria.sell() = and(Order::make / Asset::type / AssetType::nft).isEqualTo(true)

fun Criteria.bid() = and(Order::take / Asset::type / AssetType::nft).isEqualTo(true)

fun Criteria.scrollTo(continuation: String?, sort: OrderFilterSort) = when (sort) {
    OrderFilterSort.TAKE_PRICE_DESC -> {
        val price = Continuation.parse<Continuation.Price>(continuation)
        price?.let {
            logger.warn("Using deprecated field ${Order::takePriceUsd} for scrolling")
        }
        price?.let { c ->
            this.orOperator(
                Order::takePriceUsd lt c.afterPrice,
                Criteria().andOperator(
                    Order::takePriceUsd isEqualTo c.afterPrice,
                    Order::hash lt c.afterId
                )
            )
        }
    }
    OrderFilterSort.MAKE_PRICE_ASC -> {
        val price = Continuation.parse<Continuation.Price>(continuation)
        price?.let {
            logger.warn("Using deprecated field ${Order::makePriceUsd} for scrolling")
        }
        price?.let { c ->
            this.orOperator(
                Order::makePriceUsd gt c.afterPrice,
                Criteria().andOperator(
                    Order::makePriceUsd isEqualTo c.afterPrice,
                    Order::hash gt c.afterId
                )
            )
        }
    }
    OrderFilterSort.LAST_UPDATE_ASC -> {
        val lastDate = Continuation.parse<Continuation.LastDate>(continuation)
        lastDate?.let { c ->
            this.orOperator(
                Order::lastUpdateAt gt c.afterDate,
                Criteria().andOperator(
                    Order::lastUpdateAt isEqualTo c.afterDate,
                    Order::hash gt c.afterId
                )
            )
        }
    }
    OrderFilterSort.LAST_UPDATE_DESC -> {
        val lastDate = Continuation.parse<Continuation.LastDate>(continuation)
        lastDate?.let { c ->
            this.orOperator(
                Order::lastUpdateAt lt c.afterDate,
                Criteria().andOperator(
                    Order::lastUpdateAt isEqualTo c.afterDate,
                    Order::hash lt c.afterId
                )
            )
        }
    }
} ?: this

fun Criteria.scrollTo(continuation: String?, sort: OrderFilterSort, currency: Address?): Criteria {
    return if (currency == null) {
        scrollTo(continuation, sort)
    } else {
        when (sort) {
            OrderFilterSort.TAKE_PRICE_DESC -> {
                val price = Continuation.parse<Continuation.Price>(continuation)
                price?.let { c ->
                    this.orOperator(
                        Order::takePrice lt c.afterPrice,
                        Criteria().andOperator(
                            Order::takePrice isEqualTo c.afterPrice,
                            Order::hash lt c.afterId
                        )
                    )
                }
            }
            OrderFilterSort.MAKE_PRICE_ASC -> {
                val price = Continuation.parse<Continuation.Price>(continuation)
                price?.let { c ->
                    this.orOperator(
                        Order::makePrice gt c.afterPrice,
                        Criteria().andOperator(
                            Order::makePrice isEqualTo c.afterPrice,
                            Order::hash gt c.afterId
                        )
                    )
                }
            }
            else -> null
        } ?: scrollTo(continuation, sort)
    }
}
