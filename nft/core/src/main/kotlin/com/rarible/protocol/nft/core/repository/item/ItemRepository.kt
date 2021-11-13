package com.rarible.protocol.nft.core.repository.item

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import scalether.domain.Address

@Component
@CaptureSpan(type = SpanType.DB, subtype = "item")
class ItemRepository(
    private val mongo: ReactiveMongoOperations
) {
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

    companion object {
        const val COLLECTION = "item"
    }
}
