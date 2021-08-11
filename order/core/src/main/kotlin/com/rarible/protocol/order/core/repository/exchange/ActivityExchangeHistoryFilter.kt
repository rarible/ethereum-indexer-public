package com.rarible.protocol.order.core.repository.exchange

import com.rarible.core.mongo.util.div
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.misc.isSingleton
import com.rarible.protocol.order.core.model.*
import org.bson.Document
import org.springframework.data.mongodb.core.query.*
import scalether.domain.Address

sealed class ActivityExchangeHistoryFilter {
    protected companion object {
        val makeNftKey = LogEvent::data / OrderExchangeHistory::make / Asset::type / AssetType::nft
        val takeNftKey = LogEvent::data / OrderExchangeHistory::take / Asset::type / AssetType::nft
    }

    internal abstract fun getCriteria(): Criteria
    
    internal open val hint: Document? = null

    class AllSell(private val continuation: Continuation?) : ActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.ALL_SELL_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return makeNftKey isEqualTo true scrollTo continuation
        }
    }

    class AllCanceledBid(private val continuation: Continuation?) : ActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.ALL_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return takeNftKey isEqualTo true canceled true scrollTo continuation
        }
    }

    protected infix fun Criteria.canceled(isCancel: Boolean): Criteria =
        if (isCancel) {
            and(LogEvent::data / OrderExchangeHistory::type).isEqualTo(ItemType.CANCEL)
        } else {
            this
        }

    protected infix fun Criteria.scrollTo(continuation: Continuation?): Criteria =
        if (continuation == null) {
            this
        } else {
            this.orOperator(
                LogEvent::data / OrderExchangeHistory::date lt continuation.afterDate,
                (LogEvent::data / OrderExchangeHistory::date isEqualTo continuation.afterDate).and("_id")
                    .lt(continuation.afterId)
            )
        }
}

sealed class UserActivityExchangeHistoryFilter(users: List<Address>) : ActivityExchangeHistoryFilter() {
    protected companion object {
        private val makerKey = LogEvent::data / OrderSideMatch::maker
        private val takerKey = LogEvent::data / OrderSideMatch::taker
    }

    protected val makerCriteria = if (users.isSingleton) makerKey isEqualTo users.single() else makerKey inValues users
    protected val takerCriteria = if (users.isSingleton) takerKey isEqualTo users.single() else takerKey inValues users

    class ByUserSell(users: List<Address>, private val continuation: Continuation?) : UserActivityExchangeHistoryFilter(users) {
        override val hint: Document =
            if (users.isSingleton) ExchangeHistoryRepositoryIndexes.MAKER_SELL_DEFINITION.indexKeys
            else ExchangeHistoryRepositoryIndexes.ALL_SELL_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return AllSell(null).getCriteria().andOperator(makerCriteria) scrollTo continuation
        }
    }

    class ByUserCanceledBid(users: List<Address>, private val continuation: Continuation?) : UserActivityExchangeHistoryFilter(users) {
        override val hint: Document =
            if (users.isSingleton) ExchangeHistoryRepositoryIndexes.MAKER_BID_DEFINITION.indexKeys
            else ExchangeHistoryRepositoryIndexes.ALL_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return AllCanceledBid(null).getCriteria().andOperator(makerCriteria) scrollTo continuation
        }
    }

    class ByUserBuy(users: List<Address>, private val continuation: Continuation?) : UserActivityExchangeHistoryFilter(users) {
        override val hint: Document =
            if (users.isSingleton) ExchangeHistoryRepositoryIndexes.TAKER_SELL_DEFINITION.indexKeys
            else ExchangeHistoryRepositoryIndexes.ALL_SELL_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return AllSell(null).getCriteria().andOperator(takerCriteria) scrollTo continuation
        }
    }
}

sealed class CollectionActivityExchangeHistoryFilter : ActivityExchangeHistoryFilter() {
    protected companion object {
        val makeNftContractKey = LogEvent::data / OrderExchangeHistory::make / Asset::type / Erc721AssetType::token
        val takeNftContractKey = LogEvent::data / OrderExchangeHistory::take / Asset::type / Erc721AssetType::token
    }

    data class ByCollectionSell(private val contract: Address, private val continuation: Continuation?) : CollectionActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.COLLECTION_SELL_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val makeNftContractCriteria = makeNftContractKey isEqualTo contract
            return AllSell(null).getCriteria().andOperator(makeNftContractCriteria) scrollTo continuation
        }
    }

    data class ByCollectionCanceledBid(private val contract: Address, private val continuation: Continuation?) : CollectionActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.COLLECTION_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val takeNftContractCriteria = takeNftContractKey isEqualTo contract
            return AllCanceledBid(null).getCriteria().andOperator(takeNftContractCriteria) scrollTo continuation
        }
    }
}

sealed class ItemActivityExchangeHistoryFilter : CollectionActivityExchangeHistoryFilter() {
    protected companion object {
        val makeNftTokenIdKey = LogEvent::data / OrderExchangeHistory::make / Asset::type / Erc721AssetType::tokenId
        val takeNftTokenIdKey = LogEvent::data / OrderExchangeHistory::take / Asset::type / Erc721AssetType::tokenId
    }

    data class ByItemSell(private val contract: Address, private val tokenId: EthUInt256, private val continuation: Continuation?) : ItemActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.ITEM_SELL_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val makeNftContractCriteria = makeNftContractKey isEqualTo contract
            val makeNftTokenIdCriteria = makeNftTokenIdKey isEqualTo tokenId
            return Criteria().andOperator(makeNftContractCriteria, makeNftTokenIdCriteria) scrollTo continuation
        }
    }

    data class ByItemCanceledBid(private val contract: Address, private val tokenId: EthUInt256, private val continuation: Continuation?) : ItemActivityExchangeHistoryFilter() {
        override val hint: Document = ExchangeHistoryRepositoryIndexes.ITEM_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val takeNftContractCriteria = takeNftContractKey isEqualTo contract
            val takeNftTokenIdCriteria = takeNftTokenIdKey isEqualTo tokenId
            return Criteria().andOperator(takeNftContractCriteria, takeNftTokenIdCriteria) canceled true scrollTo continuation
        }
    }
}