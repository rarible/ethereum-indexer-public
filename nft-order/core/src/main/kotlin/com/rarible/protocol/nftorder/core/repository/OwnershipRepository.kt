package com.rarible.protocol.nftorder.core.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.data.ItemSellStats
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
        val criteria = Criteria("_id").isEqualTo(ownershipId)
        return template.remove(Query(criteria), collection).awaitFirstOrNull()
    }

    suspend fun getItemSellStats(itemId: ItemId): ItemSellStats {
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
        val mapping = template.find(query, Document::class.java, collection)
            // each record means 1 unique ownership
            .map { 1 to BigInteger(it.get(bestSellOrderField, Document::class.java).getString(makeStockField)) }
            .reduce { n1, n2 -> Pair(n1.first + n2.first, n1.second.plus(n2.second)) }
            .awaitFirstOrElse { Pair(0, BigInteger.ZERO) }

        return ItemSellStats(mapping.first, mapping.second)
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