package com.rarible.protocol.order.core.model.order

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.repository.order.OrderRepositoryIndexes
import org.bson.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import scalether.domain.Address

data class OrderFilterBestSellByCollectionByCurrency(
    val collection: Address,
    val currency: Address,
    override val sort: OrderFilterSort,
) : OrderFilter() {

    override val status: List<OrderStatusDto>? = listOf(OrderStatusDto.ACTIVE)
    override val platforms: List<PlatformDto> = emptyList()
    override val origin: Address? = null

    override fun toQuery(continuation: String?, limit: Int): Query {
        return Query(
            Criteria()
                .sell()
                .forCollection(collection)
                .forPlatform(platforms.mapNotNull { convert(it) })
                .fromOrigin(origin)
                .forStatus(status)
                .forCurrency(currency)
                .scrollTo(continuation, sort)
        ).limit(limit).with(sort(sort)).withHint(hint())
    }

    private fun hint(): Document = OrderRepositoryIndexes.SELL_ORDERS_BY_COLLECTION_CURRENCY_SORT_BY_PRICE_DEFINITION.indexKeys
}
