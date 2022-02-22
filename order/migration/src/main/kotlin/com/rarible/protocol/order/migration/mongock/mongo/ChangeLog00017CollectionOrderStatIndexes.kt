package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.repository.CollectionOrderStatRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "00017")
class ChangeLog00017CollectionOrderStatIndexes {

    @ChangeSet(
        id = "ChangeLog00017CollectionOrderStatIndexes.createCollectionOrderStatRepositoryIndexes",
        order = "1",
        author = "protocol",
        runAlways = true
    )
    fun createIndexForAll(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking {
        CollectionOrderStatRepository(template).createIndexes()
    }
}