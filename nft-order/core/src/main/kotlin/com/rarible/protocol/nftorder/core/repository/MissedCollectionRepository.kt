package com.rarible.protocol.nftorder.core.repository

import com.rarible.protocol.nftorder.core.model.MissedCollection
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class MissedCollectionRepository(private val template: ReactiveMongoTemplate) {

    suspend fun save(item: MissedCollection): MissedCollection {
        return template.save(item, COLLECTION).awaitFirst()
    }

    suspend fun get(id: Address): MissedCollection? {
        return template.findById<MissedCollection>(id, COLLECTION).awaitFirstOrNull()
    }

    companion object {
        const val COLLECTION = "missed_collection"
    }
}
