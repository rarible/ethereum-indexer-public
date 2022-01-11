package com.rarible.protocol.order.core.model.order

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.repository.order.OrderRepositoryIndexes
import org.bson.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import scalether.domain.Address

data class OrderFilterSell(
    override val origin: Address? = null,
    override val platforms: List<PlatformDto>,
    override val sort: OrderFilterSort,
    override val status: List<OrderStatusDto>? = null
) : OrderFilter() {

    override fun toQuery(continuation: String?, limit: Int): Query {
        return Query(
            Criteria()
                .sell()
                .forPlatform(platforms.mapNotNull { convert(it) })
                .fromOrigin(origin)
                .forStatus(status)
                .scrollTo(continuation, sort)
        ).limit(limit).with(sort(sort)).withHint(hint())
    }

    private fun hint(): Document = when {
        hasPlatforms && hasStatuses -> OrderRepositoryIndexes.SELL_ORDERS_PLATFORM_STATUS_DEFINITION.indexKeys
        hasPlatforms && !hasStatuses -> OrderRepositoryIndexes.SELL_ORDERS_PLATFORM_DEFINITION.indexKeys
        hasStatuses -> OrderRepositoryIndexes.SELL_ORDERS_STATUS_DEFINITION.indexKeys
        else -> OrderRepositoryIndexes.SELL_ORDERS_DEFINITION.indexKeys
    }
}
