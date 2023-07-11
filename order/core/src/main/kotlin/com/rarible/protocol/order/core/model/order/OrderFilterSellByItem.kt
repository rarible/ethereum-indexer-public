package com.rarible.protocol.order.core.model.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.repository.order.OrderRepositoryIndexes
import org.bson.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger

data class OrderFilterSellByItem(
    override val origin: Address? = null,
    override val platforms: List<PlatformDto>,
    override val sort: OrderFilterSort,
    override val status: List<OrderStatusDto>? = null,
    val currency: Address? = null,
    val contract: Address,
    val tokenId: BigInteger,
    val maker: Address? = null
) : OrderFilter() {

    override fun toQuery(continuation: String?, limit: Int): Query {
        return Query(
            Criteria()
                .forToken(contract, tokenId)
                .forPlatform(platforms.mapNotNull { convert(it) })
                .forMaker(maker)
                .forCurrency(currency)
                .fromOrigin(origin)
                .forStatus(status)
                .scrollTo(continuation, sort, currency)
        ).limit(limit).with(sort(sort, currency)).withHint(hint())
    }

    override fun toContinuation(order: Order) = when (sort) {
        OrderFilterSort.LAST_UPDATE_DESC,
        OrderFilterSort.LAST_UPDATE_ASC -> {
            Continuation.LastDate(order.lastUpdateAt, order.hash)
        }

        OrderFilterSort.DB_UPDATE_DESC,
        OrderFilterSort.DB_UPDATE_ASC -> {
            Continuation.LastDate(order.dbUpdatedAt ?: order.lastUpdateAt, order.hash)
        }

        OrderFilterSort.TAKE_PRICE_DESC -> {
            if (currency != null) {
                Continuation.Price(order.takePrice ?: BigDecimal.ZERO, order.hash)
            } else {
                order.takePriceUsd?.let {
                    logger.warn("Using deprecated field ${Order::takePriceUsd.name} for sorting")
                }
                Continuation.Price(order.takePriceUsd ?: BigDecimal.ZERO, order.hash)
            }
        }

        OrderFilterSort.MAKE_PRICE_ASC -> {
            if (currency != null) {
                Continuation.Price(order.makePrice ?: BigDecimal.ZERO, order.hash)
            } else {
                order.makePriceUsd?.let {
                    logger.warn("Using deprecated field ${Order::makePriceUsd.name} for sorting")
                }
                Continuation.Price(order.makePriceUsd ?: BigDecimal.ZERO, order.hash)
            }
        }
    }.toString()

    private fun Criteria.forToken(token: Address, tokenId: BigInteger): Criteria {
        return this.andOperator(
            Order::make / Asset::type / NftAssetType::token isEqualTo token,
            Order::make / Asset::type / NftAssetType::tokenId isEqualTo EthUInt256(tokenId)
            /*
            // TODO PT-1652 this works fine for floor bids since such bids include entire collection
            // But for AMM orders such approach doesn't work since they include only part of the collection
            Criteria().orOperator(
                Order::make / Asset::type / NftAssetType::tokenId isEqualTo EthUInt256(tokenId),
                Criteria().andOperator(
                    Order::make / Asset::type / NftAssetType::tokenId exists false,
                    Order::make / Asset::type / NftAssetType::nft isEqualTo true
                )
            )
            */
        )
    }

    private fun hint(): Document = when {
        currency != null -> {
            when {
                // In most cases there are not a lot of order of same maker for same item - platform/status indexes
                // might be omitted here
                //maker != null -> OrderRepositoryIndexes.SELL_ORDERS_BY_ITEM_MAKER_SORT_BY_PRICE_DEFINITION
                // The most "popular" request for "best sell order" - by item/currency/status
                !status.isNullOrEmpty() -> OrderRepositoryIndexes.SELL_ORDERS_BY_ITEM_CURRENCY_STATUS_SORT_BY_PRICE_DEFINITION
                // In other cases - use index for item/currency only (ideally platform, but not so popular query)
                else -> OrderRepositoryIndexes.SELL_ORDERS_BY_ITEM_SORT_BY_PRICE_DEFINITION
            }
        }

        platforms.isEmpty() -> OrderRepositoryIndexes.SELL_ORDERS_BY_ITEM_SORT_BY_USD_PRICE_DEFINITION
        else -> OrderRepositoryIndexes.SELL_ORDERS_BY_ITEM_PLATFORM_SORT_BY_USD_PRICE_DEFINITION

    }.indexKeys
}
