package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking

@ChangeLog(order = "00003")
class ChangeLog00003OrderVersionIndexes {
    @ChangeSet(
        id = "ChangeLog00003OrderVersionIndexes.dropUnneededOrderVersionRepositoryIndexes",
        order = "1",
        author = "protocol",
        runAlways = true
    )
    fun dropUnneededIndex(@NonLockGuarded orderVersionRepository: OrderVersionRepository) = runBlocking {
        orderVersionRepository.dropIndexes()
    }

    @ChangeSet(
        id = "ChangeLog00003OrderVersionIndexes.createOrderVersionRepositoryIndexes",
        order = "2",
        author = "protocol",
        runAlways = true
    )
    fun createIndexForAll(@NonLockGuarded orderVersionRepository: OrderVersionRepository) = runBlocking {
        orderVersionRepository.createIndexes()
    }
}
