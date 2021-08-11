package com.rarible.protocol.nftorder.core.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.model.OwnershipId
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrElse
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.*
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class OwnershipRepository(
    private val template: ReactiveMongoTemplate
) {

    private val logger = LoggerFactory.getLogger(OwnershipRepository::class.java)

    val collection: String = template.getCollectionName(Ownership::class.java)

    suspend fun save(ownership: Ownership): Ownership {
        return template.save(ownership).awaitFirst()
    }

    suspend fun get(id: OwnershipId): Ownership? {
        return template.findById<Ownership>(id).awaitFirstOrNull()
    }

    suspend fun findAll(ids: List<OwnershipId>): List<Ownership> {
        return template.find<Ownership>(
            Query(
                Ownership::id inValues ids
            ),
            Ownership::class.java
        ).collectList().awaitFirst()
    }

    suspend fun delete(ownershipId: OwnershipId): DeleteResult? {
        return template.remove(
            Query(
                Criteria().andOperator(
                    Ownership::contract isEqualTo ownershipId.token,
                    Ownership::tokenId isEqualTo ownershipId.tokenId,
                    Ownership::owner isEqualTo ownershipId.owner
                )
            ), Ownership::class.java
        ).awaitFirstOrNull()
    }

    suspend fun getTotalStock(itemId: ItemId): BigInteger {
        val bestSellOrderField = Ownership::bestSellOrder.name
        val makeStockField = OrderDto::makeStock.name
        val query = Query(
            Criteria().andOperator(
                Ownership::contract isEqualTo itemId.token,
                Ownership::tokenId isEqualTo itemId.tokenId,
                Ownership::bestSellOrder exists true
            )
        )

        query.fields().include("$bestSellOrderField.$makeStockField")

        // BigInteger stored as String, so we have to retrieve it and cast to Number manually
        return template.find(query, Document::class.java, collection)
            .map { BigInteger(it.get(bestSellOrderField, Document::class.java).getString(makeStockField)) }
            .reduce { n1, n2 -> n1.plus(n2) }
            .awaitFirstOrElse { BigInteger.ZERO }
    }

    suspend fun createIndices() = runBlocking {
        Indices.ALL.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    object Indices {

        val OWNERSHIP_CONTRACT_TOKENID: Index = Index()
            .on(Ownership::contract.name, Sort.Direction.ASC)
            .on(Ownership::tokenId.name, Sort.Direction.ASC)
            .background()

        val ALL = listOf(
            OWNERSHIP_CONTRACT_TOKENID
        )
    }
}