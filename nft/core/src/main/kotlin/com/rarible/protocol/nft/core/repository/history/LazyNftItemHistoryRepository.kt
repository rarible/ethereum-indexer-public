package com.rarible.protocol.nft.core.repository.history

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.filterIsInstance
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.LazyItemHistory
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address

@Component
@CaptureSpan(type = SpanType.DB)
class LazyNftItemHistoryRepository(
    private val mongo: ReactiveMongoOperations
) {

    fun save(lazyItemHistory: LazyItemHistory): Mono<LazyItemHistory> {
        return mongo.save(lazyItemHistory, COLLECTION)
    }

    fun remove(lazyItemHistory: LazyItemHistory): Mono<Boolean> {
        return mongo.remove(lazyItemHistory, COLLECTION).map { it.wasAcknowledged() }
    }

    fun findLazyMintById(itemId: ItemId): Flux<ItemLazyMint> {
        val criteria = tokenCriteria(itemId.token, itemId.tokenId)
        return mongo
            .find(Query(criteria).with(LOG_SORT_DESC), LazyItemHistory::class.java, COLLECTION)
            .filterIsInstance()
    }

    fun find(
        token: Address? = null,
        tokenId: EthUInt256? = null,
        from: ItemId? = null,
        to: ItemId? = null
    ): Flux<LazyItemHistory> {
        val c = tokenCriteria(token, tokenId, from, to)
        return mongo.find(Query(c).with(LOG_SORT_ASC), LazyItemHistory::class.java, COLLECTION)
    }

    suspend fun createIndexes() {
        ALL_INDEXES.forEach { index ->
            mongo.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    private fun tokenCriteria(
        token: Address?,
        tokenId: EthUInt256?,
        from: ItemId? = null,
        to: ItemId? = null
    ): Criteria {
        return when {
            token != null && tokenId != null ->
                Criteria.where(DATA_TOKEN).`is`(token).and(DATA_TOKEN_ID).`is`(tokenId)
            token != null && from != null ->
                Criteria.where(DATA_TOKEN).`is`(token).and(DATA_TOKEN_ID).gt(from.tokenId)
            token != null ->
                Criteria.where(DATA_TOKEN).`is`(token)
            from != null && to == null ->
                fromCriteria(from)
            from == null && to != null ->
                toCriteria(to)
            from != null && to != null ->
                Criteria().andOperator(fromCriteria(from), toCriteria(to))
            else ->
                Criteria()
        }
    }

    private fun fromCriteria(from: ItemId): Criteria {
        return Criteria().orOperator(
            Criteria.where(DATA_TOKEN).`is`(from.token).and(DATA_TOKEN_ID).gt(from.tokenId),
            Criteria.where(DATA_TOKEN).gt(from.token)
        )
    }

    private fun toCriteria(to: ItemId): Criteria {
        return Criteria().orOperator(
            Criteria.where(DATA_TOKEN).`is`(to.token).and(DATA_TOKEN_ID).lte(to.tokenId),
            Criteria.where(DATA_TOKEN).lt(to.token)
        )
    }

    companion object {

        const val COLLECTION = "lazy_nft_item_history"

        val DATA_TOKEN = LazyItemHistory::token.name
        val DATA_TOKEN_ID = LazyItemHistory::tokenId.name

        val LOG_SORT_ASC: Sort = Sort.by(DATA_TOKEN, DATA_TOKEN_ID, "_id")
        val LOG_SORT_DESC: Sort = LOG_SORT_ASC.descending()

        private val BY_TOKEN_TOKEN_ID_ID: Index = Index()
            .on(DATA_TOKEN, Sort.Direction.ASC)
            .on(DATA_TOKEN_ID, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        val ALL_INDEXES = listOf(
            BY_TOKEN_TOKEN_ID_ID
        )
    }
}
