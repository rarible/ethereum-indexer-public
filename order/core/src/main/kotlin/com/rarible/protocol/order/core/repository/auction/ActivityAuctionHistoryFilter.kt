package com.rarible.protocol.order.core.repository.auction

import com.rarible.core.mongo.util.div
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.continuation.DateIdContinuation
import com.rarible.protocol.order.core.continuation.PriceIdContinuation
import com.rarible.protocol.order.core.misc.safeQueryParam
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Word
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.*
import scalether.domain.Address

sealed class ActivityAuctionHistoryFilter {
    internal abstract fun getCriteria(): Criteria
    internal open val hint: Document? = null

    abstract val auctionActivitySort: AuctionActivitySort

    internal val sort: Sort
        get() = when (auctionActivitySort) {
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

    protected fun Criteria.scrollTo(sort: AuctionActivitySort, continuation: String?): Criteria =
        if (continuation == null) {
            this
        } else when (sort) {
            AuctionActivitySort.BID_DES -> {
                val lastBid = PriceIdContinuation.parse(continuation)
                lastBid?.let {
                    this.orOperator(
                        LogEvent::data / BidPlaced::bidValue lt lastBid.price,
                        (LogEvent::data / BidPlaced::bidValue isEqualTo lastBid.price).and("_id")
                            .lt(lastBid.id.safeQueryParam())
                    )
                } ?: this
            }
            AuctionActivitySort.LATEST_FIRST -> {
                val lastDate = DateIdContinuation.parse(continuation)
                lastDate?.let {
                    this.orOperator(
                        LogEvent::data / AuctionHistory::date lt lastDate.date,
                        (LogEvent::data / AuctionHistory::date isEqualTo lastDate.date).and("_id")
                            .lt(lastDate.id.safeQueryParam())
                    )
                } ?: this
            }
            AuctionActivitySort.EARLIEST_FIRST -> {
                val lastDate = DateIdContinuation.parse(continuation)
                lastDate?.let {
                    this.orOperator(
                        LogEvent::data / AuctionHistory::date gt lastDate.date,
                        (LogEvent::data / AuctionHistory::date isEqualTo lastDate.date).and("_id")
                            .gt(lastDate.id.safeQueryParam())
                    )
                } ?: this
            }
        }

    class AllAuctionBids(
        private val hash: Word,
        private val continuation: String?
    ) : ActivityAuctionHistoryFilter() {
        override val auctionActivitySort: AuctionActivitySort = AuctionActivitySort.BID_DES

        override fun getCriteria(): Criteria {
            return (LogEvent::data / AuctionHistory::hash).isEqualTo(hash)
                .and(LogEvent::data / AuctionHistory::type).isEqualTo(AuctionHistoryType.BID_PLACED)
                .scrollTo(auctionActivitySort, continuation)
        }
    }

    class AuctionAllByType(
        private val type: AuctionHistoryType,
        private val continuation: String?,
        sort: AuctionActivitySort?
    ) : ActivityAuctionHistoryFilter() {
        override val auctionActivitySort: AuctionActivitySort = sort ?: AuctionActivitySort.LATEST_FIRST

        override fun getCriteria(): Criteria {
            return (LogEvent::data / AuctionHistory::type).isEqualTo(type)
                .scrollTo(auctionActivitySort, continuation)
        }
    }
}

sealed class AuctionByUser(
    private val type: AuctionHistoryType,
    private val continuation: String?,
    sort: AuctionActivitySort
) : ActivityAuctionHistoryFilter() {

    override val auctionActivitySort: AuctionActivitySort = sort

    override fun getCriteria(): Criteria {
        return Criteria().andOperator((LogEvent::data / AuctionHistory::type) isEqualTo type, extraCriteria)
            .scrollTo(auctionActivitySort, continuation)
    }

    abstract val extraCriteria: Criteria

    class Created(
        val users: List<Address>,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByUser(AuctionHistoryType.ON_CHAIN_AUCTION, continuation, sort) {
        override val extraCriteria = (LogEvent::data / OnChainAuction::seller).inValues(users)
    }

    class Bid(
        val users: List<Address>,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByUser(AuctionHistoryType.BID_PLACED, continuation, sort) {
        override val extraCriteria = (LogEvent::data / BidPlaced::buyer).inValues(users)
    }

    class Cancel(
        val users: List<Address>,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByUser(AuctionHistoryType.AUCTION_CANCELLED, continuation, sort) {
        override val extraCriteria = (LogEvent::data / AuctionCancelled::seller).inValues(users)
    }

    class Finished(
        val users: List<Address>,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByUser(AuctionHistoryType.AUCTION_FINISHED, continuation, sort) {
        override val extraCriteria = (LogEvent::data / AuctionFinished::seller).inValues(users)
    }
}

sealed class AuctionByItem(
    protected val token: Address,
    protected val tokenId: EthUInt256,
    protected val type: AuctionHistoryType,
    protected val continuation: String?,
    sort: AuctionActivitySort
) : ActivityAuctionHistoryFilter() {

    override val auctionActivitySort: AuctionActivitySort = sort

    override fun getCriteria(): Criteria {
        var criteria = (LogEvent::data / AuctionHistory::type).isEqualTo(type)
            .and(LogEvent::data / Auction::sell / Asset::type / NftAssetType::token).isEqualTo(token)
            .and(LogEvent::data / Auction::sell / Asset::type / NftAssetType::tokenId).isEqualTo(tokenId)
        return criteria.scrollTo(auctionActivitySort, continuation)
    }

    class Created(
        token: Address,
        tokenId: EthUInt256,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByItem(token, tokenId, AuctionHistoryType.ON_CHAIN_AUCTION, continuation, sort)

    class Bid(
        token: Address,
        tokenId: EthUInt256,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByItem(token, tokenId, AuctionHistoryType.BID_PLACED, continuation, sort)

    class Cancel(
        token: Address,
        tokenId: EthUInt256,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByItem(token, tokenId, AuctionHistoryType.AUCTION_CANCELLED, continuation, sort)

    class Finished(
        token: Address,
        tokenId: EthUInt256,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByItem(token, tokenId, AuctionHistoryType.AUCTION_FINISHED, continuation, sort)
}


sealed class AuctionByCollection(
    protected val token: Address,
    protected val type: AuctionHistoryType,
    protected val continuation: String?,
    sort: AuctionActivitySort
) : ActivityAuctionHistoryFilter() {

    override val auctionActivitySort: AuctionActivitySort = sort

    override fun getCriteria(): Criteria {
        var criteria = (LogEvent::data / AuctionHistory::type).isEqualTo(type)
            .and(LogEvent::data / Auction::sell / Asset::type / NftAssetType::token).isEqualTo(token)
        return criteria.scrollTo(auctionActivitySort, continuation)
    }

    class Created(
        token: Address,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByCollection(token, AuctionHistoryType.ON_CHAIN_AUCTION, continuation, sort)

    class Bid(
        token: Address,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByCollection(token, AuctionHistoryType.BID_PLACED, continuation, sort)

    class Cancel(
        token: Address,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByCollection(token, AuctionHistoryType.AUCTION_CANCELLED, continuation, sort)

    class Finished(
        token: Address,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByCollection(token, AuctionHistoryType.AUCTION_FINISHED, continuation, sort)
}
