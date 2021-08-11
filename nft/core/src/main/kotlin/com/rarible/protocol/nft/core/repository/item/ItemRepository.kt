package com.rarible.protocol.nft.core.repository.item

import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Mono

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

    companion object {
        const val COLLECTION = "item"
    }
}
