package com.rarible.protocol.nft.core.repository.history

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.model.CreateCollection
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Flux
import scalether.domain.Address

class NftHistoryRepository(
    private val mongo: ReactiveMongoOperations
) {
    fun find(address: Address?): Flux<Pair<LogEvent, CreateCollection>> {
        val c = if (address != null) {
            Criteria.where("data._id").`is`(address)
        } else {
            Criteria()
        }
        return mongo.find<LogEvent>(Query(c).with(Sort.by("data._id")), COLLECTION)
            .map { Pair(it, it.data as CreateCollection) }
    }

    companion object {
        const val COLLECTION = "nft_history"
    }
}