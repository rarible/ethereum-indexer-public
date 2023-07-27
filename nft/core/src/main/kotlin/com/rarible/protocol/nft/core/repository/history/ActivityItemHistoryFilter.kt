package com.rarible.protocol.nft.core.repository.history

import com.rarible.core.mongo.util.div
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.misc.isSingleton
import com.rarible.protocol.nft.core.misc.safeQueryParam
import com.rarible.protocol.nft.core.model.Continuation
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.ItemType
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

sealed class ActivityItemHistoryFilter {
    protected companion object {
        val typeKey = LogEvent::data / ItemHistory::type
        val statusKey = LogEvent::status
        val ownerKey = LogEvent::data / ItemTransfer::owner
        val fromKey = LogEvent::data / ItemTransfer::from
    }

    internal abstract fun getCriteria(): Criteria
    internal abstract val sort: ActivitySort
    internal open val hint: Document? = null

    class AllBurn(override val sort: ActivitySort, private val continuation: Continuation?) : ActivityItemHistoryFilter() {

        override val hint: Document = NftItemHistoryRepositoryIndexes.TRANSFER_TO_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(ownerKey).inValues(ItemTransfer.BURN_ADDRESSES)
                .scrollTo(sort, continuation)
        }
    }

    class AllSync(
        override val sort: ActivitySort,
        private val continuation: Continuation?,
        private val reverted: Boolean
    ) : ActivityItemHistoryFilter() {

        override fun getCriteria(): Criteria {
            val status = if (reverted) LogEventStatus.REVERTED else LogEventStatus.CONFIRMED
            return (typeKey isEqualTo ItemType.TRANSFER)
                .and(statusKey).isEqualTo(status)
                .scrollTo(sort, continuation)
        }
    }

    class AllMint(override val sort: ActivitySort, private val continuation: Continuation?) : ActivityItemHistoryFilter() {
        override val hint: Document = NftItemHistoryRepositoryIndexes.TRANSFER_FROM_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(fromKey).isEqualTo(Address.ZERO())
                .scrollTo(sort, continuation)
        }
    }

    class AllTransfer(override val sort: ActivitySort, private val continuation: Continuation?) : ActivityItemHistoryFilter() {
        override val hint: Document = NftItemHistoryRepositoryIndexes.BY_TYPE_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(fromKey).ne(Address.ZERO())
                .and(ownerKey).ne(Address.ZERO())
                .scrollTo(sort, continuation)
        }
    }

    protected fun Criteria.scrollTo(sort: ActivitySort, continuation: Continuation?): Criteria =
        if (continuation == null) {
            this
        } else when (sort) {
            ActivitySort.LATEST_FIRST ->
                this.orOperator(
                    LogEvent::data / ItemHistory::date lt continuation.afterDate,
                    (LogEvent::data / ItemHistory::date isEqualTo continuation.afterDate).and("_id").lt(continuation.afterId.safeQueryParam())
                )
            ActivitySort.EARLIEST_FIRST ->
                this.orOperator(
                    LogEvent::data / ItemHistory::date gt continuation.afterDate,
                    (LogEvent::data / ItemHistory::date isEqualTo continuation.afterDate).and("_id").gt(continuation.afterId.safeQueryParam())
                )
            ActivitySort.SYNC_EARLIEST_FIRST ->
                this.orOperator(
                    LogEvent::updatedAt gt continuation.afterDate,
                    (LogEvent::updatedAt isEqualTo continuation.afterDate).and("_id")
                        .gt(continuation.afterId.safeQueryParam())
                )
            ActivitySort.SYNC_LATEST_FIRST ->
                this.orOperator(
                    LogEvent::updatedAt lt continuation.afterDate,
                    (LogEvent::updatedAt isEqualTo continuation.afterDate).and("_id")
                        .lt(continuation.afterId.safeQueryParam())
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
            return this.and(LogEvent::data / ItemHistory::date).gte(start).lte(end)
        }
        return when (activitySort) {
            ActivitySort.LATEST_FIRST -> this.and(LogEvent::data / ItemHistory::date).gte(start)
            else -> this.and(LogEvent::data / ItemHistory::date).lte(end)
        }
    }
}

sealed class UserActivityItemHistoryFilter : ActivityItemHistoryFilter() {
    protected abstract val from: Instant?
    protected abstract val to: Instant?

    class ByUserMint(
        override val sort: ActivitySort,
        private val users: List<Address>,
        override val from: Instant?,
        override val to: Instant?,
        private val continuation: Continuation?
    ) : UserActivityItemHistoryFilter() {

        override val hint: Document = NftItemHistoryRepositoryIndexes.TRANSFER_TO_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val userMintCriteria = if (users.isSingleton) ownerKey isEqualTo users.single() else ownerKey inValues users
            return AllMint(sort, null).getCriteria().andOperator(userMintCriteria)
                .dateBoundary(sort, continuation, from, to)
                .scrollTo(sort, continuation)
        }
    }

    class ByUserBurn(
        override val sort: ActivitySort,
        private val users: List<Address>,
        override val from: Instant?,
        override val to: Instant?,
        private val continuation: Continuation?
    ) : UserActivityItemHistoryFilter() {
        override val hint: Document = NftItemHistoryRepositoryIndexes.TRANSFER_FROM_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val userBurnCriteria = if (users.isSingleton) fromKey isEqualTo users.single() else fromKey inValues users
            return AllBurn(sort, null).getCriteria().andOperator(userBurnCriteria)
                .dateBoundary(sort, continuation, from, to)
                .scrollTo(sort, continuation)
        }
    }

    class ByUserTransferFrom(
        override val sort: ActivitySort,
        private val users: List<Address>,
        override val from: Instant?,
        override val to: Instant?,
        private val continuation: Continuation?
    ) : UserActivityItemHistoryFilter() {
        override val hint: Document = NftItemHistoryRepositoryIndexes.TRANSFER_FROM_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .run { if (users.isSingleton) and(fromKey).isEqualTo(users.single()) else and(fromKey).inValues(users) }
                .and(ownerKey).ne(Address.ZERO())
                .dateBoundary(sort, continuation, from, to)
                .scrollTo(sort, continuation)
        }
    }

    class ByUserTransferTo(
        override val sort: ActivitySort,
        private val users: List<Address>,
        override val from: Instant?,
        override val to: Instant?,
        private val continuation: Continuation?
    ) : UserActivityItemHistoryFilter() {
        override val hint: Document = NftItemHistoryRepositoryIndexes.TRANSFER_TO_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .run { if (users.isSingleton) and(ownerKey).isEqualTo(users.single()) else and(ownerKey).inValues(users) }
                .and(fromKey).ne(Address.ZERO())
                .dateBoundary(sort, continuation, from, to)
                .scrollTo(sort, continuation)
        }
    }
}

sealed class CollectionActivityItemHistoryFilter(protected val contract: Address) : ActivityItemHistoryFilter() {
    protected val collectionKey = LogEvent::data / ItemTransfer::token

    class ByCollectionBurn(override val sort: ActivitySort, contract: Address, private val continuation: Continuation?) : CollectionActivityItemHistoryFilter(contract) {

        override val hint: Document = NftItemHistoryRepositoryIndexes.BY_COLLECTION_OWNER_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(collectionKey).isEqualTo(contract)
                .and(ownerKey).isEqualTo(Address.ZERO())
                .scrollTo(sort, continuation)
        }
    }

    class ByCollectionMint(override val sort: ActivitySort, contract: Address, private val continuation: Continuation?) : CollectionActivityItemHistoryFilter(contract) {

        override val hint: Document = NftItemHistoryRepositoryIndexes.BY_COLLECTION_TRANSFERS_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(collectionKey).isEqualTo(contract)
                .and(fromKey).isEqualTo(Address.ZERO())
                .scrollTo(sort, continuation)
        }
    }

    class ByCollectionTransfer(override val sort: ActivitySort, contract: Address, private val continuation: Continuation?) : CollectionActivityItemHistoryFilter(contract) {

        override val hint: Document = NftItemHistoryRepositoryIndexes.BY_COLLECTION_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(collectionKey).isEqualTo(contract)
                .and(fromKey).ne(Address.ZERO())
                .and(ownerKey).ne(Address.ZERO())
                .scrollTo(sort, continuation)
        }
    }
}

sealed class ItemActivityItemHistoryFilter(contract: Address, protected val tokenId: EthUInt256) : CollectionActivityItemHistoryFilter(contract) {
    protected val tokenIdKey = LogEvent::data / ItemTransfer::tokenId

    override val hint: Document = NftItemHistoryRepositoryIndexes.BY_ITEM_DEFINITION.indexKeys

    class ByItemBurn(override val sort: ActivitySort, contract: Address, tokenId: EthUInt256, private val continuation: Continuation?) : ItemActivityItemHistoryFilter(contract, tokenId) {
        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(collectionKey).isEqualTo(contract)
                .and(tokenIdKey).isEqualTo(tokenId)
                .and(ownerKey).isEqualTo(Address.ZERO())
                .scrollTo(sort, continuation)
        }
    }

    class ByItemMint(override val sort: ActivitySort, contract: Address, tokenId: EthUInt256, private val continuation: Continuation?) : ItemActivityItemHistoryFilter(contract, tokenId) {
        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(collectionKey).isEqualTo(contract)
                .and(tokenIdKey).isEqualTo(tokenId)
                .and(fromKey).isEqualTo(Address.ZERO())
                .scrollTo(sort, continuation)
        }
    }

    class ByItemTransfer(override val sort: ActivitySort, contract: Address, tokenId: EthUInt256, private val continuation: Continuation?) : ItemActivityItemHistoryFilter(contract, tokenId) {
        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(collectionKey).isEqualTo(contract)
                .and(tokenIdKey).isEqualTo(tokenId)
                .and(fromKey).ne(Address.ZERO())
                .and(ownerKey).ne(Address.ZERO())
                .scrollTo(sort, continuation)
        }
    }
}

sealed class ItemAndOwnerActivityItemHistoryFilter(contract: Address, tokenId: EthUInt256) :
    ItemActivityItemHistoryFilter(contract, tokenId) {

    protected fun makeCriteria(
        sort: ActivitySort,
        contract: Address,
        tokenId: EthUInt256,
        continuation: Continuation?,
        expression: Criteria.() -> Criteria,
    ): Criteria {
        return (typeKey isEqualTo ItemType.TRANSFER)
            .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
            .and(collectionKey).isEqualTo(contract)
            .and(tokenIdKey).isEqualTo(tokenId)
            .expression()
            .scrollTo(sort, continuation)
    }

    class ByItemAndOwnerMint(
        override val sort: ActivitySort,
        contract: Address,
        tokenId: EthUInt256,
        private val owner: Address,
        private val continuation: Continuation?,
    ) : ItemAndOwnerActivityItemHistoryFilter(contract, tokenId) {
        override fun getCriteria(): Criteria = makeCriteria(sort, contract, tokenId, continuation) {
            and(fromKey).isEqualTo(Address.ZERO())
            and(ownerKey).isEqualTo(owner)
        }
    }

    class ByItemAndOwnerTransfer(
        override val sort: ActivitySort,
        contract: Address,
        tokenId: EthUInt256,
        private val owner: Address,
        private val continuation: Continuation?,
    ) : ItemAndOwnerActivityItemHistoryFilter(contract, tokenId) {
        override fun getCriteria(): Criteria = makeCriteria(sort, contract, tokenId, continuation) {
            and(fromKey).ne(Address.ZERO())
            and(ownerKey).isEqualTo(owner)
        }
    }
}

enum class ActivitySort(val sort: Sort) {
    LATEST_FIRST(
        Sort.by(
            Sort.Order.desc("${LogEvent::data.name}.${ItemHistory::date.name}"),
            Sort.Order.desc("_id")
        )
    ),
    EARLIEST_FIRST(
        Sort.by(
            Sort.Order.asc("${LogEvent::data.name}.${ItemHistory::date.name}"),
            Sort.Order.asc("_id")
        )
    ),
    SYNC_LATEST_FIRST(
        Sort.by(
            Sort.Order.desc(LogEvent::updatedAt.name),
            Sort.Order.desc("_id")
        )
    ),
    SYNC_EARLIEST_FIRST(
        Sort.by(
            Sort.Order.asc(LogEvent::updatedAt.name),
            Sort.Order.asc("_id")
        )
    );
}
