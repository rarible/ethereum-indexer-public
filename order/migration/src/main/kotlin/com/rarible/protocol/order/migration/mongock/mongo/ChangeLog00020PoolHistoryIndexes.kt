package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.repository.pool.PoolHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking

@ChangeLog(order = "00021")
class ChangeLog00020PoolHistoryIndexes {

    @ChangeSet(
        id = "ChangeLog000020PoolHistoryIndexes.createIndexForAll",
        order = "2",
        author = "protocol",
        runAlways = true
    )
    fun createIndexForAll(@NonLockGuarded poolHistoryRepository: PoolHistoryRepository) = runBlocking {
        poolHistoryRepository.createIndexes()
    }
}
