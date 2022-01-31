package com.rarible.protocol.nft.core.repository.item

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.repository.item.ItemRepository.Indexes.ALL_INDEXES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import scalether.domain.Address

@Component
class ItemRepository(
    private val mongo: ReactiveMongoOperations
) {
    suspend fun createIndexes() {
        ALL_INDEXES.forEach { index ->
            mongo.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    fun save(item: Item): Mono<Item> {
        return mongo.save(item)
    }

    fun findById(id: ItemId): Mono<Item> {
        return mongo.findById(id)
    }

    suspend fun search(query: Query): List<Item> {
        return mongo.query<Item>().matching(query)
            .all()
            .collectList()
            .awaitFirst()
    }

    suspend fun searchByIds(ids: Set<ItemId>): List<Item> {
        val query = Query(Item::id inValues ids)
        return mongo.query<Item>().matching(query)
            .all()
            .collectList()
            .awaitFirst()
    }

    fun findFromByToken(from: Address?): Flow<Item> {
        val criteria = Criteria().run { Item::token gt from }

        val queue = Query().addCriteria(criteria)
        queue.with(Sort.by(Sort.Direction.ASC, Item::token.name))
        return mongo.query<Item>().matching(queue).all().asFlow()
    }

    fun findTokenItems(token: Address, from: EthUInt256?): Flow<Item> {
        val criteria = when {
            from != null ->
                Criteria().andOperator(
                    Item::token isEqualTo token,
                    Item::tokenId gt from
                )
            else ->
                Item::token isEqualTo token
        }
        val queue = Query().addCriteria(criteria)
        queue.with(
            Sort.by(Sort.Direction.ASC, Item::token.name, Item::tokenId.name)
        )
        return mongo.query<Item>().matching(queue).all().asFlow()
    }

    private object Indexes {
        val BY_OWNER_DEFINITION: Index = Index()
            .on(Item::owners.name, Sort.Direction.ASC)
            .on(Item::date.name, Sort.Direction.DESC)
            .on("_id", Sort.Direction.DESC)
            .background()

        val BY_COLLECTION_DEFINITION: Index = Index()
            .on(Item::token.name, Sort.Direction.ASC)
            .on(Item::date.name, Sort.Direction.DESC)
            .on("_id", Sort.Direction.DESC)
            .background()

        val BY_TOKEN_TOKEN_ID_DEFINITION: Index = Index()
            .on(Item::token.name, Sort.Direction.ASC)
            .on(Item::tokenId.name, Sort.Direction.ASC)
            .on(Item::date.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        val BY_RECIPIENT_DEFINITION: Index = Index()
            .on("${Item::creators.name}.recipient", Sort.Direction.ASC)
            .on(Item::date.name, Sort.Direction.DESC)
            .on("_id", Sort.Direction.DESC)
            .background()

        val FOR_ALL_DEFINITION: Index = Index()
            .on(Item::date.name, Sort.Direction.DESC)
            .on("_id", Sort.Direction.DESC)
            .background()

        val BY_COLLECTION_AND_OWNER_DEFINITION: Index = Index()
            .on(Item::token.name, Sort.Direction.ASC)
            .on(Item::owners.name, Sort.Direction.ASC)
            .on(Item::date.name, Sort.Direction.DESC)
            .on("_id", Sort.Direction.DESC)
            .background()

        val ALL_INDEXES = listOf(
            BY_OWNER_DEFINITION,
            BY_COLLECTION_DEFINITION,
            BY_TOKEN_TOKEN_ID_DEFINITION,
            BY_RECIPIENT_DEFINITION,
            FOR_ALL_DEFINITION,
            BY_COLLECTION_AND_OWNER_DEFINITION
        )
    }

    companion object {
        const val COLLECTION = "item"
    }
}
