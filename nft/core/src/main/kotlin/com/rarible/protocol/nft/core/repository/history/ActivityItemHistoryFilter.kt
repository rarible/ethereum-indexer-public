package com.rarible.protocol.nft.core.repository.history

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.misc.div
import com.rarible.protocol.nft.core.misc.isSingleton
import com.rarible.protocol.nft.core.model.Continuation
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.ItemType
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.*
import scalether.domain.Address

sealed class ActivityItemHistoryFilter {
    protected companion object {
        val typeKey = LogEvent::data / ItemHistory::type
        val statusKey = LogEvent::status
        val ownerKey = LogEvent::data / ItemTransfer::owner
        val fromKey = LogEvent::data / ItemTransfer::from

        val activitySort: Sort = Sort.by(
            Sort.Order.desc("${LogEvent::data.name}.${ItemHistory::date.name}"),
            Sort.Order.desc("_id")
        )
    }

    internal abstract fun getCriteria(): Criteria

    internal open val sort: Sort? = activitySort
    internal open val hint: Document? = null

    class AllBurn(private val continuation: Continuation?) : ActivityItemHistoryFilter() {
        override val hint: Document = NftItemHistoryRepositoryIndexes.TRANSFER_TO_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo  ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(ownerKey).isEqualTo(Address.ZERO()) scrollTo continuation
        }
    }

    class AllMint(private val continuation: Continuation?) : ActivityItemHistoryFilter() {
        override val hint: Document = NftItemHistoryRepositoryIndexes.TRANSFER_FROM_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo  ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(fromKey).isEqualTo(Address.ZERO()) scrollTo continuation
        }
    }

    class AllTransfer(private val continuation: Continuation?) : ActivityItemHistoryFilter() {
        override val hint: Document = NftItemHistoryRepositoryIndexes.BY_TYPE_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo  ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(fromKey).ne(Address.ZERO())
                .and(ownerKey).ne(Address.ZERO()) scrollTo continuation
        }
    }

    protected infix fun Criteria.scrollTo(continuation: Continuation?): Criteria =
        if (continuation == null) {
            this
        } else {
            this.orOperator(
                LogEvent::data / ItemHistory::date lt continuation.afterDate,
                (LogEvent::data / ItemHistory::date isEqualTo  continuation.afterDate).and("_id").lt(continuation.afterId)
            )
        }
}

sealed class UserActivityItemHistoryFilter : ActivityItemHistoryFilter() {
    class ByUserMint(private val users: List<Address>, private val continuation: Continuation?) : UserActivityItemHistoryFilter() {
        override val hint: Document = NftItemHistoryRepositoryIndexes.TRANSFER_TO_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val userMintCriteria = if (users.isSingleton) ownerKey isEqualTo users.single() else ownerKey inValues users
            return AllMint(null).getCriteria().andOperator(userMintCriteria) scrollTo continuation
        }
    }

    class ByUserBurn(private val users: List<Address>, private val continuation: Continuation?) : UserActivityItemHistoryFilter() {
        override val hint: Document = NftItemHistoryRepositoryIndexes.TRANSFER_FROM_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val userBurnCriteria = if (users.isSingleton) fromKey isEqualTo users.single() else fromKey inValues users
            return AllBurn(null).getCriteria().andOperator(userBurnCriteria) scrollTo continuation
        }
    }

    class ByUserTransferFrom(private val users: List<Address>, private val continuation: Continuation?) : UserActivityItemHistoryFilter() {
        override val hint: Document = NftItemHistoryRepositoryIndexes.TRANSFER_FROM_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo  ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .run { if (users.isSingleton) and(fromKey).isEqualTo(users.single()) else and(fromKey).inValues(users) }
                .and(ownerKey).ne(Address.ZERO()) scrollTo continuation
        }
    }

    class ByUserTransferTo(private val users: List<Address>, private val continuation: Continuation?) : UserActivityItemHistoryFilter() {
        override val hint: Document = NftItemHistoryRepositoryIndexes.TRANSFER_TO_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo  ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .run { if (users.isSingleton) and(ownerKey).isEqualTo(users.single()) else and(ownerKey).inValues(users) }
                .and(fromKey).ne(Address.ZERO()) scrollTo continuation
        }
    }
}

sealed class CollectionActivityItemHistoryFilter(protected val contract: Address) : ActivityItemHistoryFilter() {
    protected val collectionKey = LogEvent::data / ItemTransfer::token

    override val hint: Document = NftItemHistoryRepositoryIndexes.BY_COLLECTION_DEFINITION.indexKeys

    class ByCollectionBurn(contract: Address, private val continuation: Continuation?) : CollectionActivityItemHistoryFilter(contract) {
        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo  ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(collectionKey).isEqualTo(contract)
                .and(ownerKey).isEqualTo(Address.ZERO()) scrollTo continuation
        }
    }

    class ByCollectionMint(contract: Address, private val continuation: Continuation?) : CollectionActivityItemHistoryFilter(contract) {
        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo  ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(collectionKey).isEqualTo(contract)
                .and(fromKey).isEqualTo(Address.ZERO()) scrollTo continuation
        }
    }

    class ByCollectionTransfer(contract: Address, private val continuation: Continuation?) : CollectionActivityItemHistoryFilter(contract) {
        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo  ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(collectionKey).isEqualTo(contract)
                .and(fromKey).ne(Address.ZERO())
                .and(ownerKey).ne(Address.ZERO()) scrollTo continuation
        }
    }
}

sealed class ItemActivityItemHistoryFilter(contract: Address, protected val tokenId: EthUInt256) : CollectionActivityItemHistoryFilter(contract) {
    protected val tokenIdKey = LogEvent::data / ItemTransfer::tokenId

    override val hint: Document = NftItemHistoryRepositoryIndexes.BY_ITEM_DEFINITION.indexKeys

    class ByItemBurn(contract: Address, tokenId: EthUInt256, private val continuation: Continuation?) : ItemActivityItemHistoryFilter(contract, tokenId) {
        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo  ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(collectionKey).isEqualTo(contract)
                .and(tokenIdKey).isEqualTo(tokenId)
                .and(ownerKey).isEqualTo(Address.ZERO()) scrollTo continuation
        }
    }

    class ByItemMint(contract: Address, tokenId: EthUInt256, private val continuation: Continuation?) : ItemActivityItemHistoryFilter(contract, tokenId) {
        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo  ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(collectionKey).isEqualTo(contract)
                .and(tokenIdKey).isEqualTo(tokenId)
                .and(fromKey).isEqualTo(Address.ZERO()) scrollTo continuation
        }
    }

    class ByItemTransfer(contract: Address, tokenId: EthUInt256, private val continuation: Continuation?) : ItemActivityItemHistoryFilter(contract, tokenId) {
        override fun getCriteria(): Criteria {
            return (typeKey isEqualTo  ItemType.TRANSFER)
                .and(statusKey).isEqualTo(LogEventStatus.CONFIRMED)
                .and(collectionKey).isEqualTo(contract)
                .and(tokenIdKey).isEqualTo(tokenId)
                .and(fromKey).ne(Address.ZERO())
                .and(ownerKey).ne(Address.ZERO()) scrollTo continuation
        }
    }
}