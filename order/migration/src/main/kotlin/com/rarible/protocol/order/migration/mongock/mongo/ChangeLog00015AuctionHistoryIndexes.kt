package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.repository.auction.AuctionHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking

@ChangeLog(order = "000015")
class ChangeLog00015AuctionHistoryIndexes {
    @ChangeSet(
        id = "ChangeLog00015AuctionHistoryIndexes.createIndexForAll",
        order = "1",
        author = "protocol",
        runAlways = true
    )
    fun createIndexForAll(@NonLockGuarded auctionHistoryRepository: AuctionHistoryRepository) = runBlocking {
        auctionHistoryRepository.createIndexes()
    }
}
