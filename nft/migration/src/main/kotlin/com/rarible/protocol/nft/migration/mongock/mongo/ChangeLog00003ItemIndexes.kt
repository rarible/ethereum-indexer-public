package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "00003")
class ChangeLog00003ItemIndexes {
    @ChangeSet(
        id = "ChangeLog00003ItemIndexes.createItemRepositoryIndexes",
        order = "2",
        author = "protocol",
        runAlways = true
    )
    fun createAllIndexes(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking {
        ItemRepository(template).createIndexes()
    }
}
