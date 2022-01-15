package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "00015")
class ChangeLog00015AuctionIndexes {
    @ChangeSet(
        id = "ChangeLog00015AuctionIndexes.createAuctionRepositoryIndexes",
        order = "1",
        author = "protocol",
        runAlways = true
    )
    fun createIndexForAll(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking {
        AuctionRepository(template).createIndexes()
    }
}