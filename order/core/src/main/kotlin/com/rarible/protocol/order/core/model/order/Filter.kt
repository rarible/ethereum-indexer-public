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
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import scalether.domain.Address

sealed class Filter {

    abstract val origin: Address?
    abstract val platforms: List<PlatformDto>
    abstract val sort: Sort
    abstract val status: List<OrderStatusDto>?

    abstract fun toQuery(continuation: String?, limit: Int): Query

    protected fun Criteria.fromOrigin(origin: Address?) = origin?.let {
        and("${Order::data.name}.${OrderRaribleV2DataV1::originFees.name}")
            .elemMatch(Criteria.where(Part::account.name).`is`(origin))
    } ?: this

    protected fun Criteria.forStatus(status: List<OrderStatusDto>?) =
        if (status?.isNotEmpty() == true) {
            var statuses = status.map { OrderStatus.valueOf(it.name) }.toMutableList()
            if (OrderStatus.INACTIVE in statuses) {
                statuses += listOf(OrderStatus.ENDED, OrderStatus.NOT_STARTED)
            }
            and(Order::status).inValues(statuses)
        } else this

    protected fun Criteria.forPlatform(platforms: List<Platform>): Criteria {
        return if (platforms.isEmpty()) {
            this
        } else {
            and(Order::platform).inValues(platforms)
        }
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

    protected fun Criteria.forMaker(maker: Address?) = maker?.let { and(Order::maker.name).isEqualTo(it) } ?: this

    protected fun Criteria.forCollection(collection: Address) = and("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}").isEqualTo(collection)

    protected fun Criteria.sell() = and("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}").isEqualTo(true)

    protected fun Criteria.bid() = and("${Order::take.name}.${Asset::type.name}.${AssetType::nft.name}").isEqualTo(true)

    protected fun Criteria.scrollTo(continuation: String?, sort: Sort?) = when (sort) {
        Sort.LAST_UPDATE_ASC -> {
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
        else -> {
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

    protected fun Criteria.scrollTo(continuation: String?, sort: Sort, currency: Address?): Criteria {
        return if (currency == null) {
            scrollTo(continuation, sort)
        } else {
            when (sort) {
                Sort.TAKE_PRICE_DESC -> {
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
                Sort.MAKE_PRICE_ASC -> {
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

    protected fun sort(sort: Sort): org.springframework.data.domain.Sort {
        return when (sort) {
            Sort.LAST_UPDATE_ASC -> org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC,
                Order::lastUpdateAt.name,
                Order::hash.name
            )
            else -> org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC,
                Order::lastUpdateAt.name,
                Order::hash.name
            )
        }
    }

    protected fun sort(sort: Sort, currency: Address?): org.springframework.data.domain.Sort {
        return when (sort) {
            Sort.MAKE_PRICE_ASC -> {
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.ASC,
                    (currency?.let { Order::makePrice } ?: Order::makePriceUsd).name,
                    Order::hash.name
                )
            }
            Sort.TAKE_PRICE_DESC -> {
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.DESC,
                    (currency?.let { Order::takePrice } ?: Order::takePriceUsd).name,
                    Order::hash.name
                )
            }
            else -> sort(sort)
        }
    }

    protected fun convert(platform: PlatformDto): Platform? = PlatformConverter.convert(platform)

    enum class Sort {
        LAST_UPDATE_DESC,
        LAST_UPDATE_ASC,
        TAKE_PRICE_DESC,
        MAKE_PRICE_ASC
    }

}
