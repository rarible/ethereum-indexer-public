package com.rarible.protocol.nft.core.repository.history

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.filterIsInstance
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.LazyItemHistory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
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

    fun findLazyMintById(itemId: ItemId): Mono<ItemLazyMint> {
        return find(
            token = itemId.token,
            tokenId = itemId.tokenId
        ).filterIsInstance<ItemLazyMint>().singleOrEmpty()
    }

    fun find(
        token: Address? = null,
        tokenId: EthUInt256? = null,
        from: ItemId? = null
    ): Flux<LazyItemHistory> {
        val c = tokenCriteria(token, tokenId, from)
        return mongo.find(Query(c).with(LOG_SORT_ASC), LazyItemHistory::class.java, COLLECTION)
    }

    private fun tokenCriteria(token: Address?, tokenId: EthUInt256?, from: ItemId? = null): Criteria {
        return when {
            token != null && tokenId != null ->
                Criteria.where(DATA_TOKEN).`is`(token).and(DATA_TOKEN_ID).`is`(tokenId)
            token != null && from != null ->
                Criteria.where(DATA_TOKEN).`is`(token).and(DATA_TOKEN_ID).gt(from.tokenId)
            token != null ->
                Criteria.where(DATA_TOKEN).`is`(token)
            from != null ->
                Criteria().orOperator(
                    Criteria.where(DATA_TOKEN).`is`(from.token).and(DATA_TOKEN_ID).gt(from.tokenId),
                    Criteria.where(DATA_TOKEN).gt(from.token)
                )
            else ->
                Criteria()
        }
    }

    companion object {
        const val COLLECTION = "lazy_nft_item_history"

        val DATA_TOKEN = LazyItemHistory::token.name
        val DATA_TOKEN_ID = LazyItemHistory::tokenId.name

        val LOG_SORT_ASC: Sort = Sort.by(DATA_TOKEN, DATA_TOKEN_ID, "_id")
    }
}

