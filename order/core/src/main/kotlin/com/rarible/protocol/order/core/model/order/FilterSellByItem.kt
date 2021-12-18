package com.rarible.protocol.order.core.model.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.repository.order.OrderRepositoryIndexes
import org.bson.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import scalether.domain.Address
import java.math.BigInteger

data class FilterSellByItem(
    override val origin: Address? = null,
    override val platforms: List<PlatformDto>,
    override val sort: Sort,
    override val status: List<OrderStatusDto>? = null,
    val currency: Address? = null,
    val contract: Address,
    val tokenId: BigInteger,
    val maker: Address? = null
) : Filter() {

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
        return andOperator(
            Order::make / Asset::type / NftAssetType::token isEqualTo token,
            Criteria().orOperator(
                Order::make / Asset::type / NftAssetType::tokenId isEqualTo EthUInt256(tokenId),
                Criteria("${Order::make.name}.${Asset::type.name}.${NftAssetType::tokenId.name}").exists(false)
                    .and("${Order::make.name}.${Asset::type.name}._class")
                    .isEqualTo(CollectionAssetType::class.java.name)
            )
        )
    }

    fun hint(): Document {
        return if (currency != null) {
            return OrderRepositoryIndexes.SELL_ORDERS_BY_ITEM_SORT_BY_PRICE_DEFINITION.indexKeys
        } else {
            if (platforms.isEmpty()) OrderRepositoryIndexes.SELL_ORDERS_BY_ITEM_SORT_BY_USD_PRICE_DEFINITION.indexKeys
            else OrderRepositoryIndexes.SELL_ORDERS_BY_ITEM_PLATFORM_SORT_BY_USD_PRICE_DEFINITION.indexKeys
        }
    }
}
