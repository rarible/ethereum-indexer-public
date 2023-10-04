package com.rarible.protocol.nft.core.repository

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.TokenId
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component

@Component
class TokenIdRepository(
    private val mongo: ReactiveMongoOperations,
    collectionProperties: NftIndexerProperties.CollectionProperties
) {

    private val tokenIdInitialValue = collectionProperties.tokenIdInitialValue

    suspend fun save(tokenId: TokenId): TokenId {
        return mongo.save(tokenId).awaitFirst()
    }

    suspend fun generateTokenId(id: String): Long {
        // There is no need to insert base value to DB if initialValue = 0,
        // inc() operator will work for non-existing entities
        if (tokenIdInitialValue > 0) {
            generateTokenIdWithInitialValue(id)
        }
        return mongo.findAndModify(
            Query(Criteria.where("id").`is`(id)),
            Update().inc("value", 1),
            FindAndModifyOptions.options().returnNew(true).upsert(true),
            TokenId::class.java
        ).awaitFirst().value
    }

    // In some testnets we start counting token ID's not from 0,
    // in such case we need to generate initial value before using inc()
    private suspend fun generateTokenIdWithInitialValue(id: String) {
        mongo.findAndModify(
            Query(Criteria.where("id").`is`(id)),
            Update().setOnInsert("id", id).setOnInsert("value", tokenIdInitialValue),
            FindAndModifyOptions.options().returnNew(true).upsert(true),
            TokenId::class.java
        ).awaitFirstOrNull()
    }
}
