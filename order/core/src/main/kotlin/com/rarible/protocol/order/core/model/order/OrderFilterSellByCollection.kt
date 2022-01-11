package com.rarible.protocol.order.core.model.order

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.repository.order.OrderRepositoryIndexes
import org.bson.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import scalether.domain.Address

data class OrderFilterSellByCollection(
    override val origin: Address? = null,
    override val platforms: List<PlatformDto>,
    override val sort: OrderFilterSort,
    override val status: List<OrderStatusDto>? = null,
    val collection: Address
) : OrderFilter() {

    override fun toQuery(continuation: String?, limit: Int): Query {
        return Query(
            Criteria()
                .sell()
                .forCollection(collection)
                .forPlatform(platforms.mapNotNull { convert(it) })
                .fromOrigin(origin)
                .forStatus(status)
                .scrollTo(continuation, sort)
        ).limit(limit).with(sort(sort)).withHint(hint())
    }

    private fun hint(): Document = when {
        platforms.isEmpty() -> OrderRepositoryIndexes.SELL_ORDERS_BY_COLLECTION_DEFINITION.indexKeys
        else -> OrderRepositoryIndexes.SELL_ORDERS_BY_COLLECTION_PLATFORM_DEFINITION.indexKeys
    }
}
