package com.rarible.protocol.order.core.repository.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.misc.isSingleton
import com.rarible.protocol.order.core.model.Continuation
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.*
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import scalether.domain.Address

sealed class ActivityOrderVersionFilter : OrderVersionFilter() {
    override val sort = Sort.by(
        Sort.Order.desc(OrderVersion::createdAt.name),
        Sort.Order.desc("_id")
    )

    class AllList(private val continuation: Continuation?) : ActivityOrderVersionFilter() {
        override val hint: Document = OrderVersionRepositoryIndexes.ALL_LIST_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return makeNftKey isEqualTo true scrollTo continuation
        }
    }

    class AllBid(private val continuation: Continuation?) : ActivityOrderVersionFilter() {
        override val hint: Document = OrderVersionRepositoryIndexes.ALL_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return takeNftKey isEqualTo true scrollTo continuation
        }
    }

    protected infix fun Criteria.scrollTo(continuation: Continuation?): Criteria {
        return if (continuation == null) {
            this
        } else {
            this.orOperator(
                OrderVersion::createdAt lt continuation.afterDate,
                (OrderVersion::createdAt isEqualTo continuation.afterDate).and("_id").lt(continuation.afterId)
            )
        }
    }
}

sealed class UserActivityOrderVersionFilter(users: List<Address>) : ActivityOrderVersionFilter() {
    protected val makerCriteria = if (users.isSingleton) OrderVersion::maker isEqualTo users.single() else OrderVersion::maker inValues users

    class ByUserMakeBid(users: List<Address>, private val continuation: Continuation? = null) : UserActivityOrderVersionFilter(users) {
        override val hint: Document =
            if (users.isSingleton) OrderVersionRepositoryIndexes.MAKER_BID_DEFINITION.indexKeys
            else OrderVersionRepositoryIndexes.ALL_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return AllBid(null).getCriteria().andOperator(makerCriteria) scrollTo continuation
        }
    }

    class ByUserList(users: List<Address>, val continuation: Continuation?) : UserActivityOrderVersionFilter(users) {
        override val hint: Document =
            if (users.isSingleton) OrderVersionRepositoryIndexes.MAKER_LIST_DEFINITION.indexKeys
            else OrderVersionRepositoryIndexes.ALL_LIST_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return AllList(null).getCriteria().andOperator(makerCriteria) scrollTo continuation
        }
    }
}

sealed class CollectionActivityOrderVersionFilter : ActivityOrderVersionFilter() {

    data class ByCollectionList(private val contract: Address, private val continuation: Continuation? = null) : CollectionActivityOrderVersionFilter() {
        override val hint: Document = OrderVersionRepositoryIndexes.COLLECTION_LIST_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val makeNftContractCriteria = makeNftContractKey isEqualTo contract
            return AllList(null).getCriteria().andOperator(makeNftContractCriteria) scrollTo continuation
        }
    }

    data class ByCollectionBid(private val contract: Address, private val continuation: Continuation? = null) : CollectionActivityOrderVersionFilter() {
        override val hint: Document = OrderVersionRepositoryIndexes.COLLECTION_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val takeNftContractCriteria = takeNftContractKey isEqualTo contract
            return AllBid(null).getCriteria().andOperator(takeNftContractCriteria) scrollTo continuation
        }
    }
}

sealed class ItemActivityOrderVersionFilter : CollectionActivityOrderVersionFilter() {

    data class ByItemList(private val contract: Address, private val tokenId: EthUInt256, private val continuation: Continuation? = null) : ItemActivityOrderVersionFilter() {
        override val hint: Document = OrderVersionRepositoryIndexes.ITEM_LIST_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val makeNftContractCriteria = makeNftContractKey isEqualTo contract
            val makeNftTokenIdCriteria = makeNftTokenIdKey isEqualTo tokenId
            return Criteria().andOperator(makeNftContractCriteria, makeNftTokenIdCriteria) scrollTo continuation
        }
    }

    data class ByItemBid(private val contract: Address, private val tokenId: EthUInt256, private val continuation: Continuation? = null) : ItemActivityOrderVersionFilter() {
        override val hint: Document = OrderVersionRepositoryIndexes.ITEM_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val takeNftContractCriteria = takeNftContractKey isEqualTo contract
            val takeNftTokenIdCriteria = takeNftTokenIdKey isEqualTo tokenId
            return Criteria().andOperator(takeNftContractCriteria, takeNftTokenIdCriteria) scrollTo continuation
        }
    }
}