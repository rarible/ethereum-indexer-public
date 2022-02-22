package com.rarible.protocol.order.core.model.order

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

@Deprecated("Temporary introduced for floor price search")
data class OrderFilterSellByCollectionAndCurrency(
    val currency: Address,
    val contract: Address
) : OrderFilter() {

    override val origin: Address? = null
    override val sort: OrderFilterSort = OrderFilterSort.MAKE_PRICE_ASC
    override val platforms: List<PlatformDto> = emptyList()
    override val status: List<OrderStatusDto> = listOf(OrderStatusDto.ACTIVE)

    override fun toQuery(continuation: String?, limit: Int): Query {
        return Query(
            Criteria()
                .forToken(contract)
                .forCurrency(currency)
                .forStatus(status)
                .scrollTo(continuation, sort, currency)
        ).limit(limit).with(sort(sort, currency)).withHint(hint())
    }

    override fun toContinuation(order: Order) = {
        Continuation.Price(order.makePrice ?: BigDecimal.ZERO, order.hash)
    }.toString()

    private fun Criteria.forToken(token: Address): Criteria {
        return this.andOperator(
            Order::make / Asset::type / NftAssetType::token isEqualTo token
        )
    }

    private fun hint(): Document = OrderRepositoryIndexes.SELL_ORDERS_BY_COLLECTION_CURRENCY_SORT_BY_PRICE_DEFINITION.indexKeys

}
