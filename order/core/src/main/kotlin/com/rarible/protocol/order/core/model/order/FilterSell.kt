package com.rarible.protocol.order.core.model.order

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.repository.order.OrderRepositoryIndexes
import org.bson.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import scalether.domain.Address

data class FilterSell(
    override val origin: Address? = null,
    override val platforms: List<PlatformDto>,
    override val sort: Sort,
    override val status: List<OrderStatusDto>? = null
) : Filter() {

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

    private fun hint(): Document {
        val hasPlatforms = platforms.isNotEmpty()
        val hasStatuses = !status.isNullOrEmpty()
        return if (hasPlatforms) {
            if (hasStatuses) {
                OrderRepositoryIndexes.SELL_ORDERS_PLATFORM_STATUS_DEFINITION.indexKeys
            } else {
                OrderRepositoryIndexes.SELL_ORDERS_PLATFORM_DEFINITION.indexKeys
            }
        } else {
            if (hasStatuses) {
                OrderRepositoryIndexes.SELL_ORDERS_STATUS_DEFINITION.indexKeys
            } else {
                OrderRepositoryIndexes.SELL_ORDERS_DEFINITION.indexKeys
            }
        }
    }
}
