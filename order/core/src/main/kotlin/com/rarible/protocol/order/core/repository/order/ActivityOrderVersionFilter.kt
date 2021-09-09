package com.rarible.protocol.order.core.repository.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.misc.isSingleton
import com.rarible.protocol.order.core.model.Continuation
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.sort.OrderActivitySort
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.*
import scalether.domain.Address

sealed class ActivityOrderVersionFilter : OrderVersionFilter() {

    abstract val activitySort: OrderActivitySort
    override val sort: Sort
        get() = when(activitySort) {
            OrderActivitySort.LATEST_FIRST -> Sort.by(
                Sort.Order.desc(OrderVersion::createdAt.name),
                Sort.Order.desc("_id")
            )
            OrderActivitySort.EARLIEST_FIRST -> Sort.by(
                Sort.Order.asc(OrderVersion::createdAt.name),
                Sort.Order.asc("_id")
            )
        }

    class AllList(override val activitySort: OrderActivitySort, private val continuation: Continuation?) : ActivityOrderVersionFilter() {
        override val hint: Document = OrderVersionRepositoryIndexes.ALL_LIST_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (makeNftKey isEqualTo true).scrollTo(activitySort, continuation)
        }
    }

    class AllBid(override val activitySort: OrderActivitySort, private val continuation: Continuation?) : ActivityOrderVersionFilter() {
        override val hint: Document = OrderVersionRepositoryIndexes.ALL_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (takeNftKey isEqualTo true).scrollTo(activitySort, continuation)
        }
    }

    protected fun Criteria.scrollTo(sort: OrderActivitySort, continuation: Continuation?): Criteria {
        return if (continuation == null) {
            this
        } else when (sort) {
            OrderActivitySort.LATEST_FIRST ->
                this.orOperator(
                    OrderVersion::createdAt lt continuation.afterDate,
                    (OrderVersion::createdAt isEqualTo continuation.afterDate).and("_id").lt(continuation.afterId)
                )
            OrderActivitySort.EARLIEST_FIRST ->
                this.orOperator(
                    OrderVersion::createdAt gt continuation.afterDate,
                    (OrderVersion::createdAt isEqualTo continuation.afterDate).and("_id").gt(continuation.afterId)
                )
        }
    }
}

sealed class UserActivityOrderVersionFilter(users: List<Address>) : ActivityOrderVersionFilter() {
    protected val makerCriteria = if (users.isSingleton) OrderVersion::maker isEqualTo users.single() else OrderVersion::maker inValues users

    class ByUserMakeBid(override val activitySort: OrderActivitySort, users: List<Address>, private val continuation: Continuation? = null) : UserActivityOrderVersionFilter(users) {
        override val hint: Document =
            if (users.isSingleton) OrderVersionRepositoryIndexes.MAKER_BID_DEFINITION.indexKeys
            else OrderVersionRepositoryIndexes.ALL_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return AllBid(activitySort,null).getCriteria()
                .andOperator(makerCriteria)
                .scrollTo(activitySort, continuation)
        }
    }

    class ByUserList(override val activitySort: OrderActivitySort, users: List<Address>, val continuation: Continuation?) : UserActivityOrderVersionFilter(users) {
        override val hint: Document =
            if (users.isSingleton) OrderVersionRepositoryIndexes.MAKER_LIST_DEFINITION.indexKeys
            else OrderVersionRepositoryIndexes.ALL_LIST_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return AllList(activitySort,null).getCriteria()
                .andOperator(makerCriteria)
                .scrollTo(activitySort, continuation)
        }
    }
}

sealed class CollectionActivityOrderVersionFilter : ActivityOrderVersionFilter() {

    data class ByCollectionList(override val activitySort: OrderActivitySort, private val contract: Address, private val continuation: Continuation? = null) : CollectionActivityOrderVersionFilter() {
        override val hint: Document = OrderVersionRepositoryIndexes.COLLECTION_LIST_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val makeNftContractCriteria = makeNftContractKey isEqualTo contract
            return AllList(activitySort,null).getCriteria()
                .andOperator(makeNftContractCriteria)
                .scrollTo(activitySort, continuation)
        }
    }

    data class ByCollectionBid(override val activitySort: OrderActivitySort, private val contract: Address, private val continuation: Continuation? = null) : CollectionActivityOrderVersionFilter() {
        override val hint: Document = OrderVersionRepositoryIndexes.COLLECTION_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val takeNftContractCriteria = takeNftContractKey isEqualTo contract
            return AllBid(activitySort,null).getCriteria()
                .andOperator(takeNftContractCriteria)
                .scrollTo(activitySort, continuation)
        }
    }
}

sealed class ItemActivityOrderVersionFilter : CollectionActivityOrderVersionFilter() {

    data class ByItemList(override val activitySort: OrderActivitySort, private val contract: Address, private val tokenId: EthUInt256, private val continuation: Continuation? = null) : ItemActivityOrderVersionFilter() {
        override val hint: Document = OrderVersionRepositoryIndexes.ITEM_LIST_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val makeNftContractCriteria = makeNftContractKey isEqualTo contract
            val makeNftTokenIdCriteria = makeNftTokenIdKey isEqualTo tokenId
            return Criteria().andOperator(makeNftContractCriteria, makeNftTokenIdCriteria)
                .scrollTo(activitySort, continuation)
        }
    }

    data class ByItemBid(override val activitySort: OrderActivitySort, private val contract: Address, private val tokenId: EthUInt256, private val continuation: Continuation? = null) : ItemActivityOrderVersionFilter() {
        override val hint: Document = OrderVersionRepositoryIndexes.ITEM_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val takeNftContractCriteria = takeNftContractKey isEqualTo contract
            val takeNftTokenIdCriteria = takeNftTokenIdKey isEqualTo tokenId
            return Criteria().andOperator(takeNftContractCriteria, takeNftTokenIdCriteria)
                .scrollTo(activitySort, continuation)
        }
    }
}