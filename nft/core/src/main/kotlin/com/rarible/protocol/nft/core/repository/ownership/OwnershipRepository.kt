package com.rarible.protocol.nft.core.repository.ownership

import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipId
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class OwnershipRepository(
    private val mongo: ReactiveMongoOperations
) {

    fun save(ownership: Ownership): Mono<Ownership> {
        return mongo.save(ownership)
    }

    suspend fun saveAll(ownerships: Collection<Ownership>): List<Ownership> {
        return mongo.insertAll(ownerships).collectList().awaitFirst()
    }

    suspend fun removeAll(ids: Collection<OwnershipId>): List<Ownership> {
        val criteria = Criteria.where("_id").inValues(ids)
        return mongo.findAllAndRemove<Ownership>(Query.query(criteria)).collectList().awaitFirst()
    }

    fun findById(id: OwnershipId): Mono<Ownership> {
        return mongo.findById(id)
    }

    suspend fun findAll(ids: Collection<OwnershipId>): List<Ownership> {
        val criteria = Criteria.where("_id").inValues(ids)
        return mongo.find<Ownership>(Query.query(criteria)).collectList().awaitFirst()
    }

    suspend fun search(criteria: Criteria?, size: Int, sort: Sort?): List<Ownership> {
        return query(criteria, size, sort)
    }

    suspend fun search(query: Query?): List<Ownership> {
        return mongo.query<Ownership>()
            .matching(query ?: Query())
            .all()
            .collectList()
            .awaitFirst()
    }

    fun deleteById(id: OwnershipId): Mono<Ownership> {
        return mongo.findAndRemove(Query(Criteria("_id").isEqualTo(id)), Ownership::class.java)
    }

    private suspend fun query(criteria: Criteria?, limit: Int, sort: Sort?): List<Ownership> {
        val query = Query.query(criteria ?: Criteria()).with(
            sort ?: Sort.by(
                Sort.Order.desc("date"),
                Sort.Order.desc("_id")
            )
        ).limit(limit)
        return mongo.query<Ownership>()
            .matching(query)
            .all()
            .collectList()
            .awaitFirst()
    }

    companion object {
        const val COLLECTION = "ownership"
    }
}
