package com.rarible.protocol.order.core.repository.auction

import com.rarible.protocol.order.core.model.Auction
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

class AuctionRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun save(auction: Auction): Auction {
        return template.save(auction).awaitFirst()
    }
}
