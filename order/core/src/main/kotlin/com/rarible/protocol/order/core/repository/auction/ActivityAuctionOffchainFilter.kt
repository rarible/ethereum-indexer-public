package com.rarible.protocol.order.core.repository.auction

import com.rarible.protocol.order.core.continuation.DateIdContinuation
import com.rarible.protocol.order.core.misc.safeQueryParam
import com.rarible.protocol.order.core.model.AuctionActivitySort
import com.rarible.protocol.order.core.model.AuctionOffchainHistory
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import scalether.domain.Address

sealed class ActivityAuctionOffchainFilter {

    internal abstract fun getCriteria(): Criteria
    internal open val hint: Document? = null

    abstract val auctionActivitySort: AuctionActivitySort

    internal val sort: Sort
        get() = when (auctionActivitySort) {
            AuctionActivitySort.LATEST_FIRST -> Sort.by(
                Sort.Order.desc("${AuctionOffchainHistory::date.name}"),
                Sort.Order.desc("_id")
            )
            AuctionActivitySort.EARLIEST_FIRST -> Sort.by(
                Sort.Order.asc("${AuctionOffchainHistory::date.name}"),
                Sort.Order.asc("_id")
            )
            AuctionActivitySort.BID_DES -> Sort.by(
                Sort.Order.desc("${AuctionOffchainHistory::date.name}"),
                Sort.Order.desc("_id")
            )
        }

    protected fun Criteria.scrollTo(sort: AuctionActivitySort, continuation: String?): Criteria =
        if (continuation == null) {
            this
        } else when (sort) {
            AuctionActivitySort.LATEST_FIRST -> {
                val lastDate = DateIdContinuation.parse(continuation)
                lastDate?.let {
                    this.orOperator(
                        AuctionOffchainHistory::date lt lastDate.date,
                        (AuctionOffchainHistory::date isEqualTo lastDate.date).and("_id")
                            .lt(lastDate.id.safeQueryParam())
                    )
                } ?: this
            }
            AuctionActivitySort.EARLIEST_FIRST -> {
                val lastDate = DateIdContinuation.parse(continuation)
                lastDate?.let {
                    this.orOperator(
                        AuctionOffchainHistory::date gt lastDate.date,
                        (AuctionOffchainHistory::date isEqualTo lastDate.date).and("_id")
                            .gt(lastDate.id.safeQueryParam())
                    )
                } ?: this
            }
            else -> this
        }

    class AuctionAllByType(
        private val type: AuctionOffchainHistory.Type,
        private val continuation: String?,
        sort: AuctionActivitySort?
    ) : ActivityAuctionOffchainFilter() {
        override val auctionActivitySort: AuctionActivitySort = sort ?: AuctionActivitySort.LATEST_FIRST

        override fun getCriteria(): Criteria {
            return (AuctionOffchainHistory::type).isEqualTo(type).scrollTo(auctionActivitySort, continuation)
        }
    }
}

sealed class AuctionOffchainByUser(
    val user: Address,
    private val type: AuctionOffchainHistory.Type,
    private val continuation: String?,
    sort: AuctionActivitySort
) : ActivityAuctionOffchainFilter() {

    override val auctionActivitySort: AuctionActivitySort = sort

    override fun getCriteria(): Criteria {
        return (AuctionOffchainHistory::type).isEqualTo(type)
            .andOperator(
                AuctionOffchainHistory::type isEqualTo type,
                AuctionOffchainHistory::seller isEqualTo user
            )
            .scrollTo(auctionActivitySort, continuation)
    }

    class Started(
        user: Address,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionOffchainByUser(user, AuctionOffchainHistory.Type.STARTED, continuation, sort)

    class Ended(
        user: Address,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionOffchainByUser(user, AuctionOffchainHistory.Type.ENDED, continuation, sort)
}

