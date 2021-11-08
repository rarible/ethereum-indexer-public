package com.rarible.protocol.order.core.repository.auction

import com.rarible.core.reduce.repository.DataRepository
import com.rarible.protocol.order.core.model.Auction
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById

class AuctionRepository(
    private val template: ReactiveMongoTemplate
) : DataRepository<Auction> {

    override suspend fun saveReduceResult(data: Auction) {
        template.save(data).awaitFirst()
    }

    suspend fun findById(hash: Word): Auction? {
        return template.findById<Auction>(hash).awaitFirstOrNull()
    }
}
