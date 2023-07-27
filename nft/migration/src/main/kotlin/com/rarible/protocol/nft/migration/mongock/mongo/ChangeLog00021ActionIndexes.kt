package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.repository.action.NftItemActionEventRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "00020")
class ChangeLog00021ActionIndexes {
    @ChangeSet(
        id = "ChangeLog00021ActionIndexes.createActionRepositoryIndexes",
        order = "2",
        author = "protocol",
        runAlways = true
    )
    fun createAllIndexes(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking {
        NftItemActionEventRepository(template).createIndexes()
    }
}
