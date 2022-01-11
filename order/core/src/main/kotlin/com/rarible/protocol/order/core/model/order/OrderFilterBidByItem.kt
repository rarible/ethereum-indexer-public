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
import org.springframework.data.mongodb.core.query.exists
import org.springframework.data.mongodb.core.query.isEqualTo
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger

data class OrderFilterBidByItem(
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
                .forCurrency(currency)
                .forMaker(maker)
                .fromOrigin(origin)
                .forStatus(status)
                .scrollTo(continuation, sort, currency)
        ).limit(limit).with(sort(sort, currency)).withHint(hint())
    }

    override fun toContinuation(order: Order) = when (sort) {
        OrderFilterSort.LAST_UPDATE_DESC -> {
            Continuation.LastDate(order.lastUpdateAt, order.hash)
        }
        OrderFilterSort.LAST_UPDATE_ASC -> {
            Continuation.LastDate(order.lastUpdateAt, order.hash)
        }
        OrderFilterSort.TAKE_PRICE_DESC -> {
            if (currency != null) {
                Continuation.Price(order.takePrice ?: BigDecimal.ZERO, order.hash)
            } else {
                Continuation.Price(order.takePriceUsd ?: BigDecimal.ZERO, order.hash)
            }
        }
        OrderFilterSort.MAKE_PRICE_ASC -> {
            if (currency != null) {
                Continuation.Price(order.makePrice ?: BigDecimal.ZERO, order.hash)
            } else {
                Continuation.Price(order.makePriceUsd ?: BigDecimal.ZERO, order.hash)
            }
        }
    }.toString()

    private fun Criteria.forToken(token: Address, tokenId: BigInteger): Criteria {
        return this.andOperator(
            Order::take / Asset::type / NftAssetType::token isEqualTo token,
            Criteria().orOperator(
                Order::take / Asset::type / NftAssetType::tokenId isEqualTo EthUInt256(tokenId),
                Criteria().andOperator(
                    Order::take / Asset::type / NftAssetType::tokenId exists false,
                    Order::take / Asset::type / NftAssetType::nft isEqualTo true)
            )
        )
    }

    private fun hint(): Document = when {
        currency != null -> OrderRepositoryIndexes.BIDS_BY_ITEM_DEFINITION.indexKeys
        platforms.isEmpty() -> OrderRepositoryIndexes.BIDS_BY_ITEM_DEFINITION.indexKeys
        else -> OrderRepositoryIndexes.BIDS_BY_ITEM_PLATFORM_DEFINITION.indexKeys
    }
}
