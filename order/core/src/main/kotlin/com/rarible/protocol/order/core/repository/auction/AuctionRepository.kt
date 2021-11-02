package com.rarible.protocol.order.core.repository.auction

import com.rarible.core.mongo.util.div
import com.rarible.core.reduce.repository.DataRepository
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.Document
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.*

class AuctionRepository(
    private val template: ReactiveMongoTemplate
) : DataRepository<Auction> {

    override suspend fun saveReduceResult(data: Auction) {
        template.save(data).awaitFirst()
    }

    suspend fun findById(hash: Word): Auction? {
        return template.findById<Auction>(hash).awaitFirstOrNull()
    }

    suspend fun search(filter: AuctionFilter, size: Int, continuation: String?): List<Auction> {
        val query = filter.toQuery(continuation, size)
        return template.query<Auction>()
            .matching(query)
            .all()
            .collectList()
            .awaitFirst()
    }

    private fun AuctionFilter.toQuery(continuation: String?, size: Int): Query {
        val (criteria, hint) = when (this) {
            is AuctionFilter.All -> getCriteria().withNoHint()
            is AuctionFilter.ByItem -> getCriteria().withNoHint()
            is AuctionFilter.ByCollection -> getCriteria().withNoHint()
            is AuctionFilter.BySeller -> getCriteria().withNoHint()
        }
        val query = Query(
            criteria
        )
        if (hint != null) {
            query.withHint(hint)
        }
        return query.limit(size)
    }

    private fun AuctionFilter.ByItem.getCriteria(): Criteria {
        var criteria = (Auction::sell / Asset::type / NftAssetType::token).isEqualTo(token)
            .and(Auction::sell / Asset::type / NftAssetType::tokenId).isEqualTo(tokenId)

        if (seller != null) {
            criteria = criteria.and(Auction::seller).isEqualTo(seller)
        }
        return criteria
    }

    private fun AuctionFilter.ByCollection.getCriteria(): Criteria {
        var criteria = (Auction::sell / Asset::type / NftAssetType::token).isEqualTo(token)

        if (seller != null) {
            criteria = criteria.and(Auction::seller).isEqualTo(seller)
        }
        return criteria
    }

    private fun AuctionFilter.BySeller.getCriteria(): Criteria {
        return (Auction::seller).isEqualTo(seller)
    }

    private fun AuctionFilter.All.getCriteria(): Criteria {
        return Criteria()
    }

    private infix fun Criteria.withHint(index: Document) = Pair(this, index)

    private fun Criteria.withNoHint() = Pair<Criteria, Document?>(this, null)
}

