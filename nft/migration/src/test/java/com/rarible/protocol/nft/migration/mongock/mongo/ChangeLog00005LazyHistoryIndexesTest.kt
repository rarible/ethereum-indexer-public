package com.rarible.protocol.nft.migration.mongock.mongo

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.LazyItemHistory
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

@IntegrationTest
class ChangeLog00005LazyHistoryIndexesTest : AbstractIntegrationTest() {

    private val changeLog = ChangeLog00005LazyHistoryIndexes()

    @Test
    fun `migration test`() = runBlocking<Unit> {
        val token = randomAddress()
        val tokenId = EthUInt256(randomBigInt())

        mongo.remove(Query(), LazyNftItemHistoryRepository.COLLECTION).awaitFirst()

        mongo.indexOps(LazyNftItemHistoryRepository.COLLECTION).ensureIndex(
            Index()
                .on(LazyItemHistory::token.name, Sort.Direction.ASC)
                .on(LazyItemHistory::tokenId.name, Sort.Direction.ASC)
        ).awaitFirst()

        mongo.save(
            ChangeLog00005LazyHistoryIndexes.OldItemLazyMint(
                token = token,
                tokenId = tokenId,
                value = EthUInt256(randomBigInt()),
                date = nowMillis(),
                uri = "URI",
                standard = TokenStandard.ERC721,
                creators = listOf(Part(randomAddress(), 10000)),
                royalties = emptyList(),
                signatures = emptyList()
            ),
            LazyNftItemHistoryRepository.COLLECTION
        ).awaitFirst()

        mongo.updateMulti(
            Query(Criteria()),
            Update().set("_class", "com.rarible.protocol.nft.core.model.ItemLazyMint"),
            LazyNftItemHistoryRepository.COLLECTION
        ).awaitFirst()

        changeLog.makeIdSurrogate(mongoTemplate)
        changeLog.extendTokenTokenIdIndexWithId(mongoTemplate)

        val itemHistory = lazyNftItemHistoryRepository.find(token, tokenId).single().awaitFirst()
        assertThat(itemHistory).isInstanceOf(ItemLazyMint::class.java)

        val collection = mongo.getCollection(LazyNftItemHistoryRepository.COLLECTION).awaitFirst()
        val newId = collection.find().awaitFirst()["_id"].toString()
        assertThat(ObjectId.isValid(newId)).withFailMessage(newId)
        val nextIndexes =
            mongo.indexOps(LazyNftItemHistoryRepository.COLLECTION).indexInfo
                .map { it.name }.collectList().awaitFirst().toSet()
        assertThat(nextIndexes).containsExactly("_id_", "token_1_tokenId_1__id_1")
    }
}