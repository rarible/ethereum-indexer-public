package com.rarible.protocol.nft.core.repository.token

import com.rarible.protocol.nft.core.model.TokenByteCode
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
class TokenByteCodeRepository(
    private val mongo: ReactiveMongoOperations
) {
    suspend fun save(token: TokenByteCode): TokenByteCode {
        return mongo.save(token).awaitSingle()
    }

    suspend fun get(hash: Word): TokenByteCode? {
        return mongo.findById(hash, TokenByteCode::class.java).awaitFirstOrNull()
    }

    suspend fun exist(hash: Word): Boolean {
        val criteria = Criteria.where("_id").isEqualTo(hash)
        return mongo.exists(Query(criteria), TokenByteCode.COLLECTION).awaitSingle()
    }
}