package com.rarible.protocol.order.core.repository.order

import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.order.core.misc.limit
import com.rarible.protocol.order.core.model.Order
import org.springframework.data.domain.Sort as DataSort
import org.bson.Document
import org.springframework.data.mongodb.core.query.*

sealed class OrderFilter(val sort : Sort, val size: Int?) {
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
    }

    fun toContinuation(order: Order): String {
        return when (sort) {
            Sort.LAST_UPDATE_DESC, Sort.LAST_UPDATE_ASC -> {
                Continuation.LastDate(order.lastUpdateAt, order.hash)
            }
        }.toString()
    }

    internal abstract fun getCriteria(): Criteria

    internal fun Criteria.scrollTo(sort: Sort, continuation: Continuation.LastDate?): Criteria {
        return if (continuation == null) {
            this
        } else {
            when (sort) {
                Sort.LAST_UPDATE_DESC -> {
                    this.orOperator(
                        Order::lastUpdateAt lt continuation.afterDate,
                        (Order::lastUpdateAt isEqualTo continuation.afterDate).and("_id").lt(continuation.afterId)
                    )
                }
                Sort.LAST_UPDATE_ASC -> {
                    this.orOperator(
                        Order::lastUpdateAt gt continuation.afterDate,
                        (Order::lastUpdateAt isEqualTo continuation.afterDate).and("_id").gt(continuation.afterId)
                    )
                }
            }
        }
    }

    enum class Sort {
        LAST_UPDATE_DESC,
        LAST_UPDATE_ASC
    }
}

class OrderFilterAll(
    sort : Sort,
    size: Int?,
    override val continuation: String?
) : OrderFilter(sort, size) {
    override val hint: Document = OrderRepositoryIndexes.BY_LAST_UPDATE_AND_ID_DEFINITION.indexKeys

    override fun getCriteria(): Criteria {
        return Criteria().scrollTo(sort, Continuation.parse(continuation))
    }
}
