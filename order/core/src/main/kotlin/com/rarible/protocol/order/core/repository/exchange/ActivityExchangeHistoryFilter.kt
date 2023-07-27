package com.rarible.protocol.order.core.repository.exchange

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.mongo.util.div
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.misc.isSingleton
import com.rarible.protocol.order.core.misc.safeQueryParam
import com.rarible.protocol.order.core.model.ActivitySort
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.Continuation
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.exists
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import org.springframework.data.mongodb.core.query.ne
import scalether.domain.Address
import java.time.Instant

sealed class ActivityExchangeHistoryFilter {
    protected companion object {
        val makeNftKey = ReversedEthereumLogRecord::data / OrderExchangeHistory::make / Asset::type / AssetType::nft
        val takeNftKey = ReversedEthereumLogRecord::data / OrderExchangeHistory::take / Asset::type / AssetType::nft
        val takeOrderExchange = ReversedEthereumLogRecord::data / OrderExchangeHistory::take
        val makeOrderExchange = ReversedEthereumLogRecord::data / OrderExchangeHistory::make
        val makerOrderExchange = ReversedEthereumLogRecord::data / OrderExchangeHistory::maker
        val orderSideMatchSide = ReversedEthereumLogRecord::data / OrderSideMatch::side
    }

    internal abstract fun getCriteria(): Criteria

    internal open val hint: Document? = null
    internal abstract val sort: ActivitySort
    internal open val status: EthereumBlockStatus = EthereumBlockStatus.CONFIRMED

    class AllSell(override val sort: ActivitySort, private val continuation: Continuation?) : ActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.ALL_SELL_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (makeNftKey isEqualTo true sideMatch true).scrollTo(sort, continuation)
        }
    }

    class AllSellRight(override val sort: ActivitySort, private val continuation: String?) : ActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.RIGHT_SELL_DEFINITION.indexKeys
        override fun getCriteria(): Criteria {
            return (makeNftKey isEqualTo true sideMatch true)
                .andOperator(orderSideMatchSide isEqualTo OrderSide.RIGHT)
                .apply {
                    if (continuation != null) {
                        this.and("_id").gt(ObjectId(continuation))
                    }
                }
        }
    }

    class AllSync(override val sort: ActivitySort, private val continuation: Continuation?) : ActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.BY_UPDATED_AT_FIELD.indexKeys
        override fun getCriteria(): Criteria {
            return Criteria().andOperator(
                takeOrderExchange exists true,
                makeOrderExchange exists true,
                makerOrderExchange exists true,
                orderSideMatchSide ne OrderSide.RIGHT
            ).scrollTo(sort, continuation)
        }
    }

    class AllRevertedSync(override val sort: ActivitySort, private val continuation: Continuation?) : ActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.BY_UPDATED_AT_FIELD.indexKeys
        override val status: EthereumBlockStatus = EthereumBlockStatus.REVERTED
        override fun getCriteria(): Criteria {
            return Criteria().andOperator(
                takeOrderExchange exists true,
                makeOrderExchange exists true,
                makerOrderExchange exists true,
                orderSideMatchSide ne OrderSide.RIGHT
            ).scrollTo(sort, continuation)
        }
    }

    class AllCanceledBid(override val sort: ActivitySort, private val continuation: Continuation?) : ActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.ALL_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (takeNftKey isEqualTo true canceled true).scrollTo(sort, continuation)
        }
    }

    class AllCanceledSell(override val sort: ActivitySort, private val continuation: Continuation?) : ActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.ALL_SELL_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (makeNftKey isEqualTo true canceled true).scrollTo(sort, continuation)
        }
    }

    protected infix fun Criteria.canceled(isCancel: Boolean): Criteria =
        if (isCancel) {
            and(ReversedEthereumLogRecord::data / OrderExchangeHistory::type).isEqualTo(ItemType.CANCEL)
        } else {
            this
        }

    protected infix fun Criteria.sideMatch(sideMatch: Boolean): Criteria =
        if (sideMatch) {
            and(ReversedEthereumLogRecord::data / OrderExchangeHistory::type).isEqualTo(ItemType.ORDER_SIDE_MATCH)
        } else {
            this
        }

    protected fun Criteria.scrollTo(sort: ActivitySort, continuation: Continuation?): Criteria =
        if (continuation == null) {
            this
        } else when (sort) {
            ActivitySort.LATEST_FIRST ->
                this.orOperator(
                    ReversedEthereumLogRecord::data / OrderExchangeHistory::date lt continuation.afterDate,
                    (ReversedEthereumLogRecord::data / OrderExchangeHistory::date isEqualTo continuation.afterDate).and("_id").lt(continuation.afterId.safeQueryParam())
                )
            ActivitySort.EARLIEST_FIRST ->
                this.orOperator(
                    ReversedEthereumLogRecord::data / OrderExchangeHistory::date gt continuation.afterDate,
                    (ReversedEthereumLogRecord::data / OrderExchangeHistory::date isEqualTo continuation.afterDate).and("_id").gt(continuation.afterId.safeQueryParam())
                )
            ActivitySort.SYNC_LATEST_FIRST ->
                this.orOperator(
                    ReversedEthereumLogRecord::updatedAt lt continuation.afterDate,
                    (ReversedEthereumLogRecord::updatedAt isEqualTo continuation.afterDate).and("_id").lt(continuation.afterId.safeQueryParam())
                )
            ActivitySort.SYNC_EARLIEST_FIRST ->
                this.orOperator(
                    ReversedEthereumLogRecord::updatedAt gt continuation.afterDate,
                    (ReversedEthereumLogRecord::updatedAt isEqualTo continuation.afterDate).and("_id").gt(continuation.afterId.safeQueryParam())
                )
            ActivitySort.BY_ID ->
                this.and("_id").gt(continuation.afterId.safeQueryParam())
        }

    protected fun Criteria.dateBoundary(
        activitySort: ActivitySort,
        continuation: Continuation?,
        from: Instant?,
        to: Instant?
    ): Criteria {
        if (from == null && to == null) {
            return this
        }
        val start = from ?: Instant.EPOCH
        val end = to ?: Instant.now()

        if (continuation == null) {
            return this.and(ReversedEthereumLogRecord::data / OrderExchangeHistory::date).gte(start).lte(end)
        }
        return when (activitySort) {
            ActivitySort.LATEST_FIRST -> this.and(ReversedEthereumLogRecord::data / OrderExchangeHistory::date).gte(start)
            ActivitySort.EARLIEST_FIRST -> this.and(ReversedEthereumLogRecord::data / OrderExchangeHistory::date).lte(end)
            ActivitySort.SYNC_LATEST_FIRST -> this.and(ReversedEthereumLogRecord::updatedAt).gte(start)
            ActivitySort.SYNC_EARLIEST_FIRST -> this.and(ReversedEthereumLogRecord::updatedAt).lte(end)
            ActivitySort.BY_ID -> this.and(ReversedEthereumLogRecord::id).gt(end)
        }
    }
}

sealed class UserActivityExchangeHistoryFilter(users: List<Address>) : ActivityExchangeHistoryFilter() {
    protected companion object {
        private val makerKey = ReversedEthereumLogRecord::data / OrderSideMatch::maker
        private val takerKey = ReversedEthereumLogRecord::data / OrderSideMatch::taker
    }

    protected val makerCriteria = if (users.isSingleton) makerKey isEqualTo users.single() else makerKey inValues users
    protected val takerCriteria = if (users.isSingleton) takerKey isEqualTo users.single() else takerKey inValues users

    abstract val from: Instant?
    abstract val to: Instant?

    class ByUserSell(
        override val sort: ActivitySort,
        users: List<Address>,
        override val from: Instant?,
        override val to: Instant?,
        private val continuation: Continuation?
    ) : UserActivityExchangeHistoryFilter(users) {

        override fun getCriteria(): Criteria {
            return AllSell(sort, null).getCriteria().andOperator(makerCriteria)
                .dateBoundary(sort, continuation, from, to)
                .scrollTo(sort, continuation)
        }
    }

    class ByUserCanceledBid(
        override val sort: ActivitySort,
        users: List<Address>,
        override val from: Instant?,
        override val to: Instant?,
        private val continuation: Continuation?
    ) : UserActivityExchangeHistoryFilter(users) {

        override fun getCriteria(): Criteria {
            return AllCanceledBid(sort, null).getCriteria().andOperator(makerCriteria)
                .dateBoundary(sort, continuation, from, to)
                .scrollTo(sort, continuation)
        }
    }

    class ByUserCanceledSell(
        override val sort: ActivitySort,
        users: List<Address>,
        override val from: Instant?,
        override val to: Instant?,
        private val continuation: Continuation?
    ) : UserActivityExchangeHistoryFilter(users) {

        override fun getCriteria(): Criteria {
            return AllCanceledSell(sort, null).getCriteria().andOperator(makerCriteria)
                .dateBoundary(sort, continuation, from, to)
                .scrollTo(sort, continuation)
        }
    }

    class ByUserBuy(
        override val sort: ActivitySort,
        users: List<Address>,
        override val from: Instant?,
        override val to: Instant?,
        private val continuation: Continuation?
    ) : UserActivityExchangeHistoryFilter(users) {

        override fun getCriteria(): Criteria {
            return AllSell(sort, null).getCriteria().andOperator(takerCriteria)
                .dateBoundary(sort, continuation, from, to)
                .scrollTo(sort, continuation)
        }
    }
}

sealed class CollectionActivityExchangeHistoryFilter : ActivityExchangeHistoryFilter() {
    protected companion object {
        val makeNftContractKey = ReversedEthereumLogRecord::data / OrderExchangeHistory::make / Asset::type / NftAssetType::token
        val takeNftContractKey = ReversedEthereumLogRecord::data / OrderExchangeHistory::take / Asset::type / NftAssetType::token
    }

    data class ByCollectionSell(override val sort: ActivitySort, private val contract: Address, private val continuation: Continuation?) : CollectionActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.COLLECTION_SELL_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val makeNftContractCriteria = makeNftContractKey isEqualTo contract
            return AllSell(sort, null).getCriteria().andOperator(makeNftContractCriteria)
                .scrollTo(sort, continuation)
        }
    }

    data class ByCollectionCanceledBid(override val sort: ActivitySort, private val contract: Address, private val continuation: Continuation?) : CollectionActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.COLLECTION_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val takeNftContractCriteria = takeNftContractKey isEqualTo contract
            return AllCanceledBid(sort, null).getCriteria().andOperator(takeNftContractCriteria)
                .scrollTo(sort, continuation)
        }
    }

    data class ByCollectionCanceledSell(override val sort: ActivitySort, private val contract: Address, private val continuation: Continuation?) : CollectionActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.COLLECTION_SELL_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val makeNftContractCriteria = makeNftContractKey isEqualTo contract
            return AllCanceledSell(sort, null).getCriteria().andOperator(makeNftContractCriteria)
                .scrollTo(sort, continuation)
        }
    }
}

sealed class ItemActivityExchangeHistoryFilter : CollectionActivityExchangeHistoryFilter() {
    protected companion object {
        val makeNftTokenIdKey = ReversedEthereumLogRecord::data / OrderExchangeHistory::make / Asset::type / NftAssetType::tokenId
        val takeNftTokenIdKey = ReversedEthereumLogRecord::data / OrderExchangeHistory::take / Asset::type / NftAssetType::tokenId
    }

    data class ByItemSell(override val sort: ActivitySort, private val contract: Address, private val tokenId: EthUInt256, private val continuation: Continuation?) : ItemActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.ITEM_SELL_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val makeNftContractCriteria = makeNftContractKey isEqualTo contract
            val makeNftTokenIdCriteria = makeNftTokenIdKey isEqualTo tokenId
            return Criteria().andOperator(makeNftContractCriteria, makeNftTokenIdCriteria)
                .scrollTo(sort, continuation)
        }
    }

    data class ByItemCanceledBid(override val sort: ActivitySort, private val contract: Address, private val tokenId: EthUInt256, private val continuation: Continuation?) : ItemActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.ITEM_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val takeNftContractCriteria = takeNftContractKey isEqualTo contract
            val takeNftTokenIdCriteria = takeNftTokenIdKey isEqualTo tokenId
            return (Criteria().andOperator(takeNftContractCriteria, takeNftTokenIdCriteria) canceled true)
                .scrollTo(sort, continuation)
        }
    }

    data class ByItemCanceledSell(override val sort: ActivitySort, private val contract: Address, private val tokenId: EthUInt256, private val continuation: Continuation?) : ItemActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.ITEM_SELL_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val makeNftContractCriteria = makeNftContractKey isEqualTo contract
            val makeNftTokenIdCriteria = makeNftTokenIdKey isEqualTo tokenId
            return (Criteria().andOperator(makeNftContractCriteria, makeNftTokenIdCriteria) canceled true)
                .scrollTo(sort, continuation)
        }
    }
}
