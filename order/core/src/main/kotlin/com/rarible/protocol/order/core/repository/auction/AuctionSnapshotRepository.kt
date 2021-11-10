package com.rarible.protocol.order.core.repository.auction

import com.rarible.core.reduce.repository.SnapshotRepository
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.AuctionReduceSnapshot
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById

class AuctionSnapshotRepository(
    private val template: ReactiveMongoTemplate
) : SnapshotRepository<AuctionReduceSnapshot, Auction, Long, Word> {

    override suspend fun get(key: Word): AuctionReduceSnapshot? {
        return template.findById<AuctionReduceSnapshot>(key).awaitFirstOrNull()
    }

    override suspend fun save(snapshot: AuctionReduceSnapshot): AuctionReduceSnapshot {
        return template.save(snapshot).awaitFirst()
    }
}
