package com.rarible.protocol.nft.core.repository.item

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.nft.core.misc.isEqualToItem
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemContinuation
import com.rarible.protocol.nft.core.model.ItemFilter
import com.rarible.protocol.nft.core.model.ItemFilterAll
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.repository.data.createItem
import com.rarible.protocol.nft.core.repository.item.ItemFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.test.AbstractIntegrationTest
import com.rarible.protocol.nft.core.test.IntegrationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonInt64
import org.bson.BsonString
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.updateFirst
import scalether.domain.AddressFactory
import java.time.Duration

@IntegrationTest
internal class ItemRepositoryIt : AbstractIntegrationTest() {

    @BeforeEach
    fun setupDbIndexes() = runBlocking {
        itemRepository.createIndexes()
    }

    @Test
    fun `should get item by acs date`() = runBlocking<Unit> {
        val now = nowMillis()
        val item1 = createItem().copy(date = now - Duration.ofMinutes(3))
        val item2 = createItem().copy(date = now - Duration.ofMinutes(2))
        val item3 = createItem().copy(date = now - Duration.ofMinutes(1))
        val item4 = createItem().copy(date = now)
        listOf(item1, item2, item3, item4).shuffled().forEach { itemRepository.save(it).awaitFirst() }
        val filter = ItemFilterAll(
            sort = ItemFilter.Sort.LAST_UPDATE_ASC,
            showDeleted = false
        )
        val items1 = itemRepository.search(filter.toCriteria(continuation = null, limit = 2)).toList()
        assertThat(items1).hasSize(2)
        Wait.waitAssert {
            assertThat(items1[0]).isEqualToItem(item1)
            assertThat(items1[1]).isEqualToItem(item2)
        }
        val items2 = itemRepository.search(filter.toCriteria(continuation = ItemContinuation(items1.last().date, items1.last().id), limit = 2)).toList()
        assertThat(items2).hasSize(2)
        Wait.waitAssert {
            assertThat(items2[0]).isEqualToItem(item3)
            assertThat(items2[1]).isEqualToItem(item4)
        }
    }

    @Test
    fun `should get item by desc date`() = runBlocking<Unit> {
        val now = nowMillis()
        val item1 = createItem().copy(date = now)
        val item2 = createItem().copy(date = now - Duration.ofMinutes(1))
        val item3 = createItem().copy(date = now - Duration.ofMinutes(2))
        val item4 = createItem().copy(date = now - Duration.ofMinutes(3))
        listOf(item1, item2, item3, item4).shuffled().forEach { itemRepository.save(it).awaitFirst() }
        val filter = ItemFilterAll(
            sort = ItemFilter.Sort.LAST_UPDATE_DESC,
            showDeleted = false
        )
        val items1 = itemRepository.search(filter.toCriteria(continuation = null, limit = 2)).toList()
        assertThat(items1).hasSize(2)
        Wait.waitAssert {
            assertThat(items1[0]).isEqualToItem(item1)
            assertThat(items1[1]).isEqualToItem(item2)
        }
        val items2 = itemRepository.search(filter.toCriteria(continuation = ItemContinuation(items1.last().date, items1.last().id), limit = 2)).toList()
        assertThat(items2).hasSize(2)
        Wait.waitAssert {
            assertThat(items2[0]).isEqualToItem(item3)
            assertThat(items2[1]).isEqualToItem(item4)
        }
    }

    @Test
    fun `should save and get item`() = runBlocking<Unit> {
        val item = createItem()

        itemRepository.save(item).awaitFirst()
        val doc = mongo.findOne<Document>(Query(), "item").awaitFirst()
        assertThat(doc["_id"])
            .isEqualTo(item.id.stringValue)

        val savedItem = itemRepository.findById(item.id).awaitFirstOrNull()
        assertThat(savedItem).isEqualToItem(item)
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
        ).awaitFirstOrNull()

        val savedItem = itemRepository.findById(item.id).awaitFirstOrNull()
        assertThat(savedItem).isEqualToItem(item.copy(royalties = listOf(Part(create, 100))))
    }
}
