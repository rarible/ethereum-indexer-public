package com.rarible.protocol.nftorder.core.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.ItemId
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
class ItemRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun save(item: Item): Item {
        return template.save(item, COLLECTION).awaitFirst()
    }

    suspend fun get(id: ItemId): Item? {
        return template.findById<Item>(id, COLLECTION).awaitFirstOrNull()
    }

    suspend fun findAll(ids: List<ItemId>): List<Item> {
        val criteria = Criteria("_id").inValues(ids)
        return template.find<Item>(Query(criteria), COLLECTION).collectList().awaitFirst()
    }

    suspend fun delete(itemId: ItemId): DeleteResult? {
        val criteria = Criteria("_id").isEqualTo(itemId)
        return template.remove(Query(criteria), COLLECTION).awaitFirstOrNull()
    }

    companion object {
        const val COLLECTION = "item"
    }
}