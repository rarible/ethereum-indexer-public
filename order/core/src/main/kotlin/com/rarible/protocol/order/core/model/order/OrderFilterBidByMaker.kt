package com.rarible.protocol.order.core.model.order

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.repository.order.OrderRepositoryIndexes
import org.bson.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import scalether.domain.Address

data class OrderFilterBidByMaker(
    override val origin: Address? = null,
    override val platforms: List<PlatformDto>,
    override val sort: OrderFilterSort,
    override val status: List<OrderStatusDto>? = null,
    val maker: Address
) : OrderFilter() {

    override fun toQuery(continuation: String?, limit: Int): Query {
        return Query(
            Criteria()
                .bid()
                .forMaker(maker)
                .forPlatform(platforms.mapNotNull { convert(it) })
                .fromOrigin(origin)
                .forStatus(status)
                .scrollTo(continuation, sort)
        ).limit(limit).with(sort(sort)).withHint(hint())
    }

    private fun hint(): Document = when {
        platforms.isEmpty() -> OrderRepositoryIndexes.BIDS_BY_MAKER_DEFINITION.indexKeys
        else -> OrderRepositoryIndexes.BIDS_BY_MAKER_PLATFORM_DEFINITION.indexKeys
    }
}
