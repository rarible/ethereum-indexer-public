package com.rarible.protocol.nft.api.e2e.items

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createItem
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.time.Instant
import kotlin.random.Random.Default.nextBoolean

// Test mongo throws "operation exceeded time limit" exception
// This test could be slow and flaky
@Disabled
@End2EndTest
class MongoTimeoutFt : SpringContainerBaseTest() {

    init {
        System.setProperty(
            "rarible.core.mongo.maxTime.medium", "1" // decrease time limit
        )
    }

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @BeforeEach
    fun clearIndexes() = runBlocking<Unit> {
        // remove items to force slow requests
        mongo.indexOps(ItemRepository.COLLECTION).dropAllIndexes().awaitSingleOrNull()
    }

    @Test
    fun `should find all items`() = runBlocking<Unit> {
        val token = AddressFactory.create()

        (0..10_000).map {
            async { saveItem(token, it, AddressFactory.create(), listOf(AddressFactory.create()), nextBoolean()) }
        }.awaitAll()

        assertThrows<NftItemControllerApi.ErrorGetNftItemsByCollection> {
            runBlocking {
                nftItemApiClient.getNftItemsByCollection(token.prefixed(), null, null, 100).awaitSingle()
            }
        }
    }

    private suspend fun saveItem(
        token: Address,
        id: Int,
        creator: Address,
        owners: List<Address>,
        deleted: Boolean = false,
        date: Instant = nowMillis()
    ) {
        val item = createItem(token, creator, owners, deleted, date).copy(tokenId = EthUInt256.of(id))
        saveItem(item)
    }

    private suspend fun saveItem(vararg items: Item) {
        items.forEach { itemRepository.save(it).awaitFirst() }
    }
}
