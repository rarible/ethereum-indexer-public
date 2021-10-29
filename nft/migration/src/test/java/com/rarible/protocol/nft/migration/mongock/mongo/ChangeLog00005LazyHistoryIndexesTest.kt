package com.rarible.protocol.nft.migration.mongock.mongo

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import scalether.domain.Address
import java.time.Instant

@IntegrationTest
class ChangeLog00005LazyHistoryIndexesTest : AbstractIntegrationTest() {

    private val changeLog = ChangeLog00005LazyHistoryIndexes()

    @Document(value = LazyNftItemHistoryRepository.COLLECTION)
    data class OldFormatItemLazyMint(
        val token: Address,
        val tokenId: EthUInt256,
        val value: EthUInt256,
        val date: Instant,
        val uri: String,
        val standard: TokenStandard,
        val creators: List<Part>,
        val royalties: List<Part>,
        val signatures: List<Binary>
    ) {
        @get:Id
        @get:AccessType(AccessType.Type.PROPERTY)
        var id: String = ItemId(token, tokenId).stringValue
    }

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
            OldFormatItemLazyMint(
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

        changeLog.extendTokenTokenIdIndexWithId(mongoTemplate)

        val itemHistory = lazyNftItemHistoryRepository.find(token, tokenId).single().awaitFirst()
        assertThat(itemHistory).isInstanceOf(ItemLazyMint::class.java)

        val nextIndexes =
            mongo.indexOps(LazyNftItemHistoryRepository.COLLECTION).indexInfo
                .map { it.name }.collectList().awaitFirst().toSet()
        assertThat(nextIndexes).containsExactly("_id_", "token_1_tokenId_1__id_1")
    }
}