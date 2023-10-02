package com.rarible.protocol.order.core.repository

import com.rarible.protocol.order.core.model.TopCollection
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class TopCollectionRepository(
    private val mongo: ReactiveMongoTemplate
) {

    suspend fun getAll(): List<Address> {
        return mongo.findAll(TopCollection::class.java)
            .map { it.id }
            .collectList().awaitFirst()
    }

    suspend fun save(token: Address) {
        mongo.save(TopCollection(token)).awaitFirst()
    }
}
