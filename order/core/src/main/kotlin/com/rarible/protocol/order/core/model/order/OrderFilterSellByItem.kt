package com.rarible.protocol.order.core.model.order

import com.rarible.ethereum.domain.EthUInt256
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

    private fun Criteria.forToken(token: Address, tokenId: BigInteger): Criteria {
        return this.andOperator(
            Order::make / Asset::type / NftAssetType::token isEqualTo token,
            Criteria().orOperator(
                Order::make / Asset::type / NftAssetType::tokenId isEqualTo EthUInt256(tokenId),
                Criteria().andOperator(
                    Order::make / Asset::type / NftAssetType::tokenId exists false,
                    Order::make / Asset::type / NftAssetType::nft isEqualTo true
                )
            )
        )
    }

    private fun hint(): Document = when {
        currency != null -> OrderRepositoryIndexes.SELL_ORDERS_BY_ITEM_SORT_BY_PRICE_DEFINITION.indexKeys
        platforms.isEmpty() -> OrderRepositoryIndexes.SELL_ORDERS_BY_ITEM_SORT_BY_USD_PRICE_DEFINITION.indexKeys
        else -> OrderRepositoryIndexes.SELL_ORDERS_BY_ITEM_PLATFORM_SORT_BY_USD_PRICE_DEFINITION.indexKeys
    }
}
