package com.rarible.protocol.nft.core.repository.item

import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.repository.data.createItem
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonInt64
import org.bson.BsonString
import org.bson.Document
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.updateFirst
import scalether.domain.AddressFactory

@IntegrationTest
internal class ItemRepositoryIt : AbstractIntegrationTest() {

    @Test
    fun `should save and get item`() = runBlocking<Unit> {
        val item = createItem()

        itemRepository.save(item).awaitFirst()
        val doc = mongo.findOne<Document>(Query(), "item").awaitFirst()
        assertThat(doc["_id"])
            .isEqualTo(item.id.stringValue)

        val savedItem = itemRepository.findById(item.id).awaitFirstOrNull()
        assertThat(savedItem).isEqualTo(item)
    }

    @Test
    fun `should read longs`() = runBlocking<Unit> {
        val item = createItem().copy(royalties = emptyList())

        itemRepository.save(item).awaitFirst()
        val create = AddressFactory.create()
        val royalties = BsonArray(listOf(
            BsonDocument("recipient", BsonString(create.toString()))
                .append("value", BsonInt64(100))
        ))
        mongo.updateFirst<Item>(
            Query(Item::id isEqualTo item.id),
            Update().set("royalties", royalties)
        ).block()

        val savedItem = itemRepository.findById(item.id).awaitFirstOrNull()
        assertThat(savedItem).isEqualTo(item.copy(royalties = listOf(Part(create, 100))))
    }

}
