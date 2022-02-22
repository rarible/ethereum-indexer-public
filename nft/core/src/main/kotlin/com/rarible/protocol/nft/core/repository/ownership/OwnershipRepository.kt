package com.rarible.protocol.nft.core.repository.ownership

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.math.BigInteger

@Component
class OwnershipRepository(
    private val mongo: ReactiveMongoOperations
) {

    fun save(ownership: Ownership): Mono<Ownership> {
        return mongo.save(ownership)
    }

    fun findById(id: OwnershipId): Mono<Ownership> {
        return mongo.findById(id)
    }

    suspend fun findAll(ids: Collection<OwnershipId>): List<Ownership> {
        val criteria = Criteria.where("_id").inValues(ids)
        return mongo.find<Ownership>(Query.query(criteria)).collectList().awaitFirst()
    }

    suspend fun findAllNotDeleted(): Flow<Pair<OwnershipId, BigInteger>> {
        val query = Query(
            Ownership::deleted isEqualTo false
        )
        query.fields().include(
            "_id",
            Ownership::value.name
        )
        query.with(Sort.by("_id"))

        return mongo.find(query, Document::class.java, COLLECTION).map {
            val id = Ownership.parseId(it.getString("_id"))
            val value = EthUInt256.of(it.getString(Ownership::value.name)).value
            Pair(id, value)
        }.asFlow()
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

    suspend fun searchAsFlow(query: Query?): Flow<Ownership> {
        return mongo.query<Ownership>()
            .matching(query ?: Query())
            .all()
            .asFlow()
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
