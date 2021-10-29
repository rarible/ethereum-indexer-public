package com.rarible.protocol.order.core.repository.auction

import com.rarible.protocol.order.core.model.Auction
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById

class AuctionRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun save(auction: Auction): Auction {
        return template.save(auction).awaitFirst()
    }

    suspend fun findById(hash: Word): Auction? {
        return template.findById<Auction>(hash).awaitFirstOrNull()
    }
}
