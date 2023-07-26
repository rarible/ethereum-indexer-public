package com.rarible.protocol.order.core.repository.auction

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.mongo.util.div
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.continuation.DateIdContinuation
import com.rarible.protocol.order.core.continuation.PriceIdContinuation
import com.rarible.protocol.order.core.misc.safeQueryParam
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.AuctionActivitySort
import com.rarible.protocol.order.core.model.AuctionCancelled
import com.rarible.protocol.order.core.model.AuctionFinished
import com.rarible.protocol.order.core.model.AuctionHistory
import com.rarible.protocol.order.core.model.AuctionHistoryType
import com.rarible.protocol.order.core.model.BidPlaced
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.OnChainAuction
import io.daonomic.rpc.domain.Word
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import scalether.domain.Address
import java.time.Instant

sealed class ActivityAuctionHistoryFilter {
    internal abstract fun getCriteria(): Criteria
    internal open val hint: Document? = null

    abstract val auctionActivitySort: AuctionActivitySort

    internal val sort: Sort
        get() = when (auctionActivitySort) {
            AuctionActivitySort.LATEST_FIRST -> Sort.by(
                Sort.Order.desc("${ReversedEthereumLogRecord::data.name}.${AuctionHistory::date.name}"),
                Sort.Order.desc("_id")
            )
            AuctionActivitySort.EARLIEST_FIRST -> Sort.by(
                Sort.Order.asc("${ReversedEthereumLogRecord::data.name}.${AuctionHistory::date.name}"),
                Sort.Order.asc("_id")
            )
            AuctionActivitySort.BID_DES -> Sort.by(
                Sort.Order.desc("${ReversedEthereumLogRecord::data.name}.${BidPlaced::bidValue.name}"),
                Sort.Order.desc("_id")
            )
            AuctionActivitySort.SYNC_LATEST_FIRST -> Sort.by(
                Sort.Order.desc(ReversedEthereumLogRecord::updatedAt.name),
                Sort.Order.desc("_id")
            )
            AuctionActivitySort.SYNC_EARLIEST_FIRST -> Sort.by(
                Sort.Order.asc(ReversedEthereumLogRecord::updatedAt.name),
                Sort.Order.asc("_id")
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
                        ReversedEthereumLogRecord::data / BidPlaced::bidValue lt lastBid.price,
                        (ReversedEthereumLogRecord::data / BidPlaced::bidValue isEqualTo lastBid.price).and("_id")
                            .lt(lastBid.id.safeQueryParam())
                    )
                } ?: this
            }
            AuctionActivitySort.LATEST_FIRST -> {
                val lastDate = DateIdContinuation.parse(continuation)
                lastDate?.let {
                    this.orOperator(
                        ReversedEthereumLogRecord::data / AuctionHistory::date lt lastDate.date,
                        (ReversedEthereumLogRecord::data / AuctionHistory::date isEqualTo lastDate.date).and("_id")
                            .lt(lastDate.id.safeQueryParam())
                    )
                } ?: this
            }
            AuctionActivitySort.EARLIEST_FIRST -> {
                val lastDate = DateIdContinuation.parse(continuation)
                lastDate?.let {
                    this.orOperator(
                        ReversedEthereumLogRecord::data / AuctionHistory::date gt lastDate.date,
                        (ReversedEthereumLogRecord::data / AuctionHistory::date isEqualTo lastDate.date).and("_id")
                            .gt(lastDate.id.safeQueryParam())
                    )
                } ?: this
            }
            AuctionActivitySort.SYNC_LATEST_FIRST -> {
                val lastDate = DateIdContinuation.parse(continuation)
                lastDate?.let {
                    this.orOperator(
                        ReversedEthereumLogRecord::updatedAt lt lastDate.date,
                        (ReversedEthereumLogRecord::updatedAt isEqualTo lastDate.date).and("_id")
                            .lt(lastDate.id.safeQueryParam())
                    )
                } ?: this
            }
            AuctionActivitySort.SYNC_EARLIEST_FIRST -> {
                val lastDate = DateIdContinuation.parse(continuation)
                lastDate?.let {
                    this.orOperator(
                        ReversedEthereumLogRecord::updatedAt gt lastDate.date,
                        (ReversedEthereumLogRecord::updatedAt isEqualTo lastDate.date).and("_id")
                            .gt(lastDate.id.safeQueryParam())
                    )
                } ?: this
            }
        }

    class AllSync(
        private val continuation: String?,
        override val auctionActivitySort: AuctionActivitySort
    ) : ActivityAuctionHistoryFilter() {
        override fun getCriteria(): Criteria {
            return Criteria()
                .scrollTo(auctionActivitySort, continuation)
        }
    }

    class AllAuctionBids(
        private val hash: Word,
        private val continuation: String?
    ) : ActivityAuctionHistoryFilter() {
        override val auctionActivitySort: AuctionActivitySort = AuctionActivitySort.BID_DES

        override fun getCriteria(): Criteria {
            return (ReversedEthereumLogRecord::data / AuctionHistory::hash).isEqualTo(hash)
                .and(ReversedEthereumLogRecord::data / AuctionHistory::type).isEqualTo(AuctionHistoryType.BID_PLACED)
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
            return (ReversedEthereumLogRecord::data / AuctionHistory::type).isEqualTo(type)
                .scrollTo(auctionActivitySort, continuation)
        }
    }
}

sealed class AuctionByUser(
    private val type: AuctionHistoryType,
    private val continuation: String?,
    sort: AuctionActivitySort
) : ActivityAuctionHistoryFilter() {

    abstract val from: Instant?
    abstract val to: Instant?
    override val auctionActivitySort: AuctionActivitySort = sort

    override fun getCriteria(): Criteria {
        return Criteria().andOperator((ReversedEthereumLogRecord::data / AuctionHistory::type) isEqualTo type, extraCriteria)
            .dateBoundary(auctionActivitySort, continuation, from, to)
            .scrollTo(auctionActivitySort, continuation)
    }

    abstract val extraCriteria: Criteria

    class Created(
        val users: List<Address>,
        override val from: Instant?,
        override val to: Instant?,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByUser(AuctionHistoryType.ON_CHAIN_AUCTION, continuation, sort) {
        override val extraCriteria = (ReversedEthereumLogRecord::data / OnChainAuction::seller).inValues(users)
    }

    class Bid(
        val users: List<Address>,
        override val from: Instant?,
        override val to: Instant?,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByUser(AuctionHistoryType.BID_PLACED, continuation, sort) {
        override val extraCriteria = (ReversedEthereumLogRecord::data / BidPlaced::buyer).inValues(users)
    }

    class Cancel(
        val users: List<Address>,
        override val from: Instant?,
        override val to: Instant?,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByUser(AuctionHistoryType.AUCTION_CANCELLED, continuation, sort) {
        override val extraCriteria = (ReversedEthereumLogRecord::data / AuctionCancelled::seller).inValues(users)
    }

    class Finished(
        val users: List<Address>,
        override val from: Instant?,
        override val to: Instant?,
        continuation: String?,
        sort: AuctionActivitySort
    ) : AuctionByUser(AuctionHistoryType.AUCTION_FINISHED, continuation, sort) {
        override val extraCriteria = (ReversedEthereumLogRecord::data / AuctionFinished::seller).inValues(users)
    }

    private fun Criteria.dateBoundary(
        activitySort: AuctionActivitySort,
        continuation: String?,
        from: Instant?,
        to: Instant?
    ): Criteria {
        if (from == null && to == null) {
            return this
        }
        val start = from ?: Instant.EPOCH
        val end = to ?: Instant.now()

        if (continuation == null) {
            return this.and(ReversedEthereumLogRecord::data / AuctionHistory::date).gte(start).lte(end)
        }
        return when (activitySort) {
            AuctionActivitySort.LATEST_FIRST -> this.and(ReversedEthereumLogRecord::data / AuctionHistory::date).gte(start)
            AuctionActivitySort.EARLIEST_FIRST -> this.and(ReversedEthereumLogRecord::data / AuctionHistory::date).lte(end)
            else -> this
        }
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
        var criteria = (ReversedEthereumLogRecord::data / AuctionHistory::type).isEqualTo(type)
            .and(ReversedEthereumLogRecord::data / Auction::sell / Asset::type / NftAssetType::token).isEqualTo(token)
            .and(ReversedEthereumLogRecord::data / Auction::sell / Asset::type / NftAssetType::tokenId).isEqualTo(tokenId)
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
        var criteria = (ReversedEthereumLogRecord::data / AuctionHistory::type).isEqualTo(type)
            .and(ReversedEthereumLogRecord::data / Auction::sell / Asset::type / NftAssetType::token).isEqualTo(token)
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
