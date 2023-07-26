package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "00001")
class ChangeLog00001OrderIndexes {
    @ChangeSet(
        id = "ChangeLog00001OrderIndexes.dropUnneededOrderRepositoryIndexes",
        order = "1",
        author = "protocol",
        runAlways = true
    )
    fun dropUnneededIndexForAll(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking {
        MongoOrderRepository(template).dropIndexes()
    }

    @ChangeSet(
        id = "ChangeLog00001OrderIndexes.createOrderRepositoryIndexes",
        order = "2",
        author = "protocol",
        runAlways = true
    )
    fun createIndexForAll(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking {
        MongoOrderRepository(template).createIndexes()
    }
}
