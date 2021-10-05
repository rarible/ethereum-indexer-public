package com.rarible.protocol.order.core.repository.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.order.core.misc.limit
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import org.springframework.data.domain.Sort as DataSort
import org.bson.Document
import org.springframework.data.mongodb.core.query.*
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant

sealed class OrderFilter(
    val sort: Sort,
    val size: Int?,
    val contract: Address? = null,
    val tokenId: EthUInt256? = null,
    val maker: Address? = null,
    val origin: Address? = null,
    val platform: Platform? = null,
    val statuses: List<OrderStatus> = listOf(),
    val start: Instant? = null,
    val end: Instant? = null
) {
    abstract val continuation: String?

    internal val limit = size.limit()
    internal abstract val hint: Document?

    internal val dataSort: DataSort = when (sort) {
        Sort.LAST_UPDATE_DESC -> DataSort.by(
            DataSort.Order.desc(Order::lastUpdateAt.name),
            DataSort.Order.desc("_id")
        )
        Sort.LAST_UPDATE_ASC -> DataSort.by(
            DataSort.Order.asc(Order::lastUpdateAt.name),
            DataSort.Order.asc("_id")
        )
        Sort.MAKE_PRICE_ASC -> DataSort.by(
            DataSort.Order.asc(Order::makePriceUsd.name),
            DataSort.Order.asc("_id")
        )
    }

    fun toContinuation(order: Order): String {
        return when (sort) {
            Sort.LAST_UPDATE_DESC, Sort.LAST_UPDATE_ASC -> {
                Continuation.LastDate(order.lastUpdateAt, order.hash)
            }
            Sort.MAKE_PRICE_ASC -> {
                Continuation.Price(order.makePriceUsd ?: BigDecimal.ZERO, order.hash)
            }
        }.toString()
    }

    fun getCriteria(): Criteria {
        return Criteria().scrollTo(sort, continuation)
    }

    internal fun Criteria.scrollTo(sort: Sort, continuationTxt: String?): Criteria {
        return if (continuation == null) {
            this
        } else {
            when (sort) {
                Sort.LAST_UPDATE_DESC -> {
                    val continuation = Continuation.parse<Continuation.LastDate>(continuationTxt)!!
                    this.orOperator(
                        Order::lastUpdateAt lt continuation.afterDate,
                        (Order::lastUpdateAt isEqualTo continuation.afterDate).and("_id").lt(continuation.afterId)
                    )
                }
                Sort.LAST_UPDATE_ASC -> {
                    val continuation = Continuation.parse<Continuation.LastDate>(continuationTxt)!!
                    this.orOperator(
                        Order::lastUpdateAt gt continuation.afterDate,
                        (Order::lastUpdateAt isEqualTo continuation.afterDate).and("_id").gt(continuation.afterId)
                    )
                }
                Sort.MAKE_PRICE_ASC -> {
                    val continuation = Continuation.parse<Continuation.Price>(continuation)!!
                    this.orOperator(
                        Order::makePriceUsd gt continuation.afterPrice,
                        Criteria().andOperator(
                            Order::makePriceUsd isEqualTo continuation.afterPrice,
                            Order::hash gt continuation.afterId
                        )
                    )
                }
            }
        }
    }

    enum class Sort {
        LAST_UPDATE_DESC,
        LAST_UPDATE_ASC,
        MAKE_PRICE_ASC
    }
}

class OrderFilterAll(
    sort: Sort,
    size: Int?,
    override val continuation: String?,
    statuses: List<OrderStatus> = listOf(),
    start: Instant? = null,
    end: Instant? = null
) : OrderFilter(
    sort = sort,
    size = size,
    statuses = statuses,
    start = start,
    end = end
) {

    override val hint: Document = OrderRepositoryIndexes.BY_LAST_UPDATE_AND_ID_DEFINITION.indexKeys
}

class OrderFilterSell(
    sort: Sort,
    size: Int?,
    override val continuation: String?,
    contract: Address? = null,
    tokenId: EthUInt256? = null,
    maker: Address? = null,
    origin: Address?,
    platform: Platform?,
    statuses: List<OrderStatus> = listOf(),
    start: Instant? = null,
    end: Instant? = null
) : OrderFilter(
    sort = sort,
    size = size,
    contract = contract,
    tokenId = tokenId,
    maker = maker,
    origin = origin,
    platform = platform,
    statuses = statuses,
    start = start,
    end = end
) {

    override val hint = null
}
