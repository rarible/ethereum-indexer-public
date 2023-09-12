package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.repository.history.RoyaltiesHistoryRepository
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "00007")
class ChangeLog00007RoyaltiesIndexes {
    @ChangeSet(
        id = "ChangeLog00007RoyaltiesIndexes.createAllIndexes",
        order = "1",
        author = "protocol",
        runAlways = true
    )
    fun createAllIndexes(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking {
        RoyaltiesHistoryRepository(template).createIndexes()
    }
}
