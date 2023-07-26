package com.rarible.protocol.nft.core.repository.inconsistentitem

import com.rarible.protocol.nft.core.model.InconsistentItem
import com.rarible.protocol.nft.core.model.InconsistentItemContinuation
import com.rarible.protocol.nft.core.model.InconsistentItemFilter
import com.rarible.protocol.nft.core.model.InconsistentItemFilterAll
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.isEqualTo

object InconsistentItemFilterCriteria {
    private const val DEFAULT_LIMIT = 1_000

    fun InconsistentItemFilter.toCriteria(continuation: InconsistentItemContinuation?, limit: Int?): Query {
        val criteria = when (this) {
            is InconsistentItemFilterAll -> all()
        }.scrollTo(continuation, this.sort)

        return Query.query(criteria)
            .with(this.sort.toMongoSort())
            .limit(limit ?: DEFAULT_LIMIT)
    }

    private fun all() = Criteria()

    private fun InconsistentItemFilter.Sort.toMongoSort() =
        when (this) {
            InconsistentItemFilter.Sort.LAST_UPDATE_ASC -> Sort.by(
                Sort.Order.asc(InconsistentItem::lastUpdatedAt.name),
                Sort.Order.asc("_id")
            )
        }

    private fun Criteria.scrollTo(
        continuation: InconsistentItemContinuation?,
        sort: InconsistentItemFilter.Sort
    ): Criteria =
        if (continuation == null) {
            this
        } else {
            when (sort) {
                InconsistentItemFilter.Sort.LAST_UPDATE_ASC -> {
                    this.orOperator(
                        InconsistentItem::lastUpdatedAt gt continuation.afterDate,
                        Criteria().andOperator(
                            InconsistentItem::lastUpdatedAt isEqualTo continuation.afterDate,
                            InconsistentItem::id gt continuation.afterId
                        )
                    )
                }
            }
        }
}
