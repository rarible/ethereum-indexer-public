package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "00006")
class ChangeLog00006TokenIndexes {
    @ChangeSet(
        id = "ChangeLog00006TokenIndexes.createTokenRepositoryIndexes",
        order = "1",
        author = "protocol",
        runAlways = true
    )
    fun createAllIndexes(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking {
        TokenRepository(template).createIndexes()
    }
}
