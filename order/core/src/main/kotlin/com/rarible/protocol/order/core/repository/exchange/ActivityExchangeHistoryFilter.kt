package com.rarible.protocol.order.core.repository.exchange

import com.rarible.core.mongo.util.div
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.misc.isSingleton
import com.rarible.protocol.order.core.model.*
import org.bson.Document
import org.springframework.data.mongodb.core.query.*
import scalether.domain.Address
import java.time.Instant

sealed class ActivityExchangeHistoryFilter {
    protected companion object {
        val makeNftKey = LogEvent::data / OrderExchangeHistory::make / Asset::type / AssetType::nft
        val takeNftKey = LogEvent::data / OrderExchangeHistory::take / Asset::type / AssetType::nft
    }

    internal abstract fun getCriteria(): Criteria

    internal open val hint: Document? = null
    internal abstract val sort: ActivitySort

    class AllSell(override val sort: ActivitySort, private val continuation: Continuation?) : ActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.ALL_SELL_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (makeNftKey isEqualTo true).scrollTo(sort, continuation)
        }
    }

    class AllCanceledBid(override val sort: ActivitySort, private val continuation: Continuation?) : ActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.ALL_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (takeNftKey isEqualTo true canceled true).scrollTo(sort, continuation)
        }
    }

    protected infix fun Criteria.canceled(isCancel: Boolean): Criteria =
        if (isCancel) {
            and(LogEvent::data / OrderExchangeHistory::type).isEqualTo(ItemType.CANCEL)
        } else {
            this
        }

    protected fun Criteria.scrollTo(sort: ActivitySort, continuation: Continuation?): Criteria =
        if (continuation == null) {
            this
        } else when (sort) {
            ActivitySort.LATEST_FIRST ->
                this.orOperator(
                    LogEvent::data / OrderExchangeHistory::date lt continuation.afterDate,
                    (LogEvent::data / OrderExchangeHistory::date isEqualTo continuation.afterDate).and("_id").lt(continuation.afterId)
                )
            ActivitySort.EARLIEST_FIRST ->
                this.orOperator(
                    LogEvent::data / OrderExchangeHistory::date gt continuation.afterDate,
                    (LogEvent::data / OrderExchangeHistory::date isEqualTo continuation.afterDate).and("_id").gt(continuation.afterId)
                )
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
            return this.and(LogEvent::data / OrderExchangeHistory::date).gte(start).lte(end)
        }
        return when (activitySort) {
            ActivitySort.LATEST_FIRST -> this.and(LogEvent::data / OrderExchangeHistory::date).gte(start)
            ActivitySort.EARLIEST_FIRST -> this.and(LogEvent::data / OrderExchangeHistory::date).lte(end)
        }
    }
}

sealed class UserActivityExchangeHistoryFilter(users: List<Address>) : ActivityExchangeHistoryFilter() {
    protected companion object {
        private val makerKey = LogEvent::data / OrderSideMatch::maker
        private val takerKey = LogEvent::data / OrderSideMatch::taker
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

        override val hint: Document =
            if (users.isSingleton) ExchangeHistoryRepositoryIndexes.MAKER_SELL_DEFINITION.indexKeys
            else ExchangeHistoryRepositoryIndexes.ALL_SELL_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return AllSell(sort,null).getCriteria().andOperator(makerCriteria)
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

        override val hint: Document =
            if (users.isSingleton) ExchangeHistoryRepositoryIndexes.MAKER_BID_DEFINITION.indexKeys
            else ExchangeHistoryRepositoryIndexes.ALL_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return AllCanceledBid(sort,null).getCriteria().andOperator(makerCriteria)
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
        override val hint: Document =
            if (users.isSingleton) ExchangeHistoryRepositoryIndexes.TAKER_SELL_DEFINITION.indexKeys
            else ExchangeHistoryRepositoryIndexes.ALL_SELL_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return AllSell(sort,null).getCriteria().andOperator(takerCriteria)
                .dateBoundary(sort, continuation, from, to)
                .scrollTo(sort, continuation)
        }
    }
}

sealed class CollectionActivityExchangeHistoryFilter : ActivityExchangeHistoryFilter() {
    protected companion object {
        val makeNftContractKey = LogEvent::data / OrderExchangeHistory::make / Asset::type / NftAssetType::token
        val takeNftContractKey = LogEvent::data / OrderExchangeHistory::take / Asset::type / NftAssetType::token
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
}

sealed class ItemActivityExchangeHistoryFilter : CollectionActivityExchangeHistoryFilter() {
    protected companion object {
        val makeNftTokenIdKey = LogEvent::data / OrderExchangeHistory::make / Asset::type / NftAssetType::tokenId
        val takeNftTokenIdKey = LogEvent::data / OrderExchangeHistory::take / Asset::type / NftAssetType::tokenId
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
}
