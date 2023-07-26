package com.rarible.protocol.nft.core.repository.ownership

import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address

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

    fun searchAsFlow(query: Query?): Flow<Ownership> {
        return mongo.query<Ownership>()
            .matching(query ?: Query())
            .all()
            .asFlow()
    }

    fun findByOwner(owner: Address, fromIdExcluded: OwnershipId? = null): Flow<Ownership> {
        return mongo.query<Ownership>()
            .matching(
                Query.query(
                    Criteria().andOperator(
                        listOfNotNull(
                            Ownership::owner isEqualTo owner,
                            fromIdExcluded?.let { Ownership::id gt fromIdExcluded }
                        )
                    )
                ).with(
                    Sort.by(
                        Sort.Order.asc(Ownership::id.name)
                    )
                )
            )
            .all()
            .asFlow()
    }

    @Deprecated("Not to be actively used, as we don't typically remove ownerships")
    fun deleteById(id: OwnershipId): Mono<Ownership> {
        return mongo.findAndRemove(Query(Criteria("_id").isEqualTo(id)), Ownership::class.java)
    }

    /**
     * Delete all ownerships by itemId
     *
     * @param itemId    Item ID
     * @return flux of deleted ownerships
     */
    @Deprecated("Not to be actively used, as we don't typically remove ownerships")
    fun deleteAllByItemId(itemId: ItemId): Flux<Ownership> {
        val query =
            Query(where(Ownership::token).isEqualTo(itemId.token).and(Ownership::tokenId).isEqualTo(itemId.tokenId))
        return mongo.findAllAndRemove(query, Ownership::class.java, COLLECTION)
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
