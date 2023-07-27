package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking

@ChangeLog(order = "00002")
class ChangeLog00002OrderHistoryIndexes {
    @ChangeSet(
        id = "ChangeLog00002OrderHistoryIndexes.dropUnneededOrderHistoryRepositoryIndexes",
        order = "1",
        author = "protocol",
        runAlways = true
    )
    fun dropUnneededIndexes(@NonLockGuarded exchangeHistoryRepository: ExchangeHistoryRepository) = runBlocking {
        exchangeHistoryRepository.dropIndexes()
    }

    @ChangeSet(
        id = "ChangeLog00002OrderHistoryIndexes.createOrderHistoryRepositoryIndexes",
        order = "2",
        author = "protocol",
        runAlways = true
    )
    fun createIndexForAll(@NonLockGuarded exchangeHistoryRepository: ExchangeHistoryRepository) = runBlocking {
        exchangeHistoryRepository.createIndexes()
    }
}
