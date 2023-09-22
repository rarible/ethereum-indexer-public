package com.rarible.protocol.nft.core.repository.token

import com.rarible.protocol.nft.core.model.TokenByteCode
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.Document
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
class TokenByteCodeRepository(
    private val mongo: ReactiveMongoOperations
) {
    suspend fun createIndexes() {
        TokenByteCodeIndexes.ALL_INDEXES.forEach { index ->
            mongo.indexOps(TokenByteCode.COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun save(token: TokenByteCode): TokenByteCode {
        return mongo.save(token).awaitSingle()
    }

    suspend fun get(hash: Word): TokenByteCode? {
        return mongo.findById(hash, TokenByteCode::class.java).awaitFirstOrNull()
    }

    suspend fun allScamHashes(): List<Word> {
        val query = query(where("scam").isEqualTo(true))
        query.fields().include("_id")
        return mongo.find(query, Document::class.java, TokenByteCode.COLLECTION)
            .map { Word.apply(it["_id"] as String) }
            .collectList().awaitSingle()
    }

    suspend fun exist(hash: Word): Boolean {
        val criteria = where("_id").isEqualTo(hash)
        return mongo.exists(Query(criteria), TokenByteCode.COLLECTION).awaitSingle()
    }
}
