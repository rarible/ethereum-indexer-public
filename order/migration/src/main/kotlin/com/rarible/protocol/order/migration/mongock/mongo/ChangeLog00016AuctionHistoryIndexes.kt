package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.repository.auction.AuctionHistoryRepository
import com.rarible.protocol.order.core.repository.auction.AuctionOffchainHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking

@ChangeLog(order = "000016")
class ChangeLog00016AuctionHistoryIndexes {

    @ChangeSet(
        id = "ChangeLog00016AuctionHistoryIndexes.createIndexForAuctionHistory",
        order = "1",
        author = "protocol",
        runAlways = true
    )
    fun createIndexForAuctionHistory(@NonLockGuarded auctionHistoryRepository: AuctionHistoryRepository) = runBlocking {
        auctionHistoryRepository.createIndexes()
    }

    @ChangeSet(
        id = "ChangeLog00016AuctionHistoryIndexes.createIndexForAuctionOffchainHistory",
        order = "2",
        author = "protocol",
        runAlways = true
    )
    fun createIndexForAuctionOffchainHistory(@NonLockGuarded auctionOffchainHistoryRepository: AuctionOffchainHistoryRepository) = runBlocking {
        auctionOffchainHistoryRepository.createIndexes()
    }
}
