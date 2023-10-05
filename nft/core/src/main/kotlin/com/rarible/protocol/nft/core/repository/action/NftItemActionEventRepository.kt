package com.rarible.protocol.nft.core.repository.action

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Action
import com.rarible.protocol.nft.core.model.ActionState
import com.rarible.protocol.nft.core.model.ActionType
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.action.NftItemActionRepositoryIndexes.ALL_INDEXES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lte
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address
import java.time.Instant

@Component
class NftItemActionEventRepository(
    private val mongo: ReactiveMongoOperations
) {
    suspend fun createIndexes() {
        ALL_INDEXES.forEach { index ->
            mongo.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    fun save(action: Action): Mono<Action> {
        return mongo.save(action, COLLECTION)
    }

    fun findPendingActions(actionAt: Instant): Flow<Action> {
        val criteria = Criteria().andOperator(
            Action::state isEqualTo ActionState.PENDING,
            Action::actionAt lte actionAt
        )
        return mongo.find<Action>(Query.query(criteria), COLLECTION).asFlow()
    }

    suspend fun findByItemIdAndType(itemId: ItemId, type: ActionType): List<Action> {
        val criteria = Criteria().andOperator(
            Action::token isEqualTo itemId.token,
            Action::tokenId isEqualTo itemId.tokenId,
            Action::type isEqualTo type,
        )
        return mongo.find<Action>(Query.query(criteria), COLLECTION).collectList().awaitFirst()
    }

    fun find(
        token: Address? = null,
        tokenId: EthUInt256? = null,
        from: ItemId? = null
    ): Flux<Action> {
        val c = tokenCriteria(token, tokenId, from)
        return mongo.find(Query(c).with(LOG_SORT_ASC), Action::class.java, COLLECTION)
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
        const val COLLECTION = "nft_item_action"

        val DATA_TOKEN = Action::token.name
        val DATA_TOKEN_ID = Action::tokenId.name

        val LOG_SORT_ASC: Sort = Sort.by(DATA_TOKEN, DATA_TOKEN_ID, "_id")
    }
}
