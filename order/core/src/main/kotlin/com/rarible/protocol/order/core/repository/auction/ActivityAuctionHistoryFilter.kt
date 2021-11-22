package com.rarible.protocol.order.core.repository.auction

import com.rarible.core.mongo.util.div
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Word
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.*

sealed class ActivityAuctionHistoryFilter {
    internal abstract fun getCriteria(): Criteria
    internal open val hint: Document? = null

    abstract val auctionActivitySort: AuctionActivitySort

    internal val sort: Sort
        get() = when(auctionActivitySort) {
            AuctionActivitySort.LATEST_FIRST -> Sort.by(
                Sort.Order.desc("${LogEvent::data.name}.${AuctionHistory::date.name}"),
                Sort.Order.desc("_id")
            )
            AuctionActivitySort.EARLIEST_FIRST -> Sort.by(
                Sort.Order.asc("${LogEvent::data.name}.${AuctionHistory::date.name}"),
                Sort.Order.asc("_id")
            )
            AuctionActivitySort.BID_DES -> Sort.by(
                Sort.Order.desc("${LogEvent::data.name}.${BidPlaced::bidValue.name}"),
                Sort.Order.desc("_id")
            )
        }

    protected fun Criteria.scrollTo(sort: AuctionActivitySort, continuation:  String?): Criteria =
        if (continuation == null) {
            this
        } else when (sort) {
            AuctionActivitySort.BID_DES -> {
                val lastBid = Continuation.parse<Continuation.Price>(continuation)
                lastBid?.let {
                    this.orOperator(
                        LogEvent::data / BidPlaced::bidValue lt lastBid.afterPrice,
                        (LogEvent::data / BidPlaced::bidValue isEqualTo lastBid.afterPrice).and("_id").lt(lastBid.afterId)
                    )
                } ?: this
            }
            AuctionActivitySort.LATEST_FIRST -> {
                val lastDate = Continuation.parse<Continuation.LastDate>(continuation)
                lastDate?.let {
                    this.orOperator(
                        LogEvent::data / AuctionHistory::date lt lastDate.afterDate,
                        (LogEvent::data / AuctionHistory::date isEqualTo lastDate.afterDate).and("_id").lt(lastDate.afterId)
                    )
                } ?: this
            }
            AuctionActivitySort.EARLIEST_FIRST -> {
                val lastDate = Continuation.parse<Continuation.LastDate>(continuation)
                lastDate?.let {
                    this.orOperator(
                        LogEvent::data / AuctionHistory::date gt lastDate.afterDate,
                        (LogEvent::data / AuctionHistory::date isEqualTo lastDate.afterDate).and("_id").gt(lastDate.afterId)
                    )
                } ?: this
            }
        }

    class AllAuctionBids(
        private val hash: Word,
        private val continuation: String?
    ): ActivityAuctionHistoryFilter() {
        override val auctionActivitySort: AuctionActivitySort = AuctionActivitySort.BID_DES

        override fun getCriteria(): Criteria {
            return (LogEvent::data / AuctionHistory::hash).isEqualTo(hash)
                .and(LogEvent::data / AuctionHistory::type).isEqualTo(AuctionHistoryType.BID_PLACED)
                .scrollTo(auctionActivitySort, continuation)
        }
    }
}
