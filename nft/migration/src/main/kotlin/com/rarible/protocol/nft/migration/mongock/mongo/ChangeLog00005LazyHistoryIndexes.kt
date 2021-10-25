package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.LazyItemHistory
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import scalether.domain.Address
import java.time.Instant

@ChangeLog(order = "00005")
class ChangeLog00005LazyHistoryIndexes {

    private val logger = LoggerFactory.getLogger(ChangeLog00005LazyHistoryIndexes::class.java)

    @Document(value = LazyNftItemHistoryRepository.COLLECTION)
    data class OldItemLazyMint(
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

    @ChangeSet(
        id = "ChangeLog00005LazyHistoryIndexes.makeIdSurrogate",
        order = "1",
        author = "protocol"
    )
    fun makeIdSurrogate(@NonLockGuarded mongo: ReactiveMongoTemplate) = runBlocking {
        val query = Query(Criteria()).with(
            Sort.by(
                LazyNftItemHistoryRepository.DATA_TOKEN,
                LazyNftItemHistoryRepository.DATA_TOKEN_ID
            )
        )
        mongo.find(query, OldItemLazyMint::class.java, NftItemHistoryRepository.COLLECTION).asFlow()
            .collect { oldHistory ->
                oldHistory.id = ObjectId().toString()
                mongo.save(oldHistory).awaitFirst()
            }
    }

    @ChangeSet(
        id = "ChangeLog00005LazyHistoryIndexes.extendTokenTokenIdIndexWithId",
        order = "2",
        author = "protocol"
    )
    fun extendTokenTokenIdIndexWithId(@NonLockGuarded mongo: ReactiveMongoTemplate) = runBlocking {
        val indexOps = mongo.indexOps(LazyNftItemHistoryRepository.COLLECTION)
        val existingIndexes = indexOps.indexInfo.map { it.name }.collectList().awaitFirst()
        logger.info("Existing indexes: $existingIndexes")
        indexOps.dropIndex("token_1_tokenId_1").awaitFirstOrNull()
        indexOps.ensureIndex(
            Index()
                .on(LazyItemHistory::token.name, Sort.Direction.ASC)
                .on(LazyItemHistory::tokenId.name, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
        ).awaitFirstOrNull()
    }
}