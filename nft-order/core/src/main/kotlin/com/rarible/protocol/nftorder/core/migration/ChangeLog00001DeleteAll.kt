package com.rarible.protocol.nftorder.core.migration

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.Ownership
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "00004")
class ChangeLog00001DeleteAll() {

    @ChangeSet(
        id = "ChangeLog00004DeleteAll.deleteAll_1",
        order = "4",
        author = "protocol",
        runAlways = false
    )
    fun deleteAll(@NonLockGuarded mongoTemplate: ReactiveMongoTemplate) = runBlocking<Unit> {
        mongoTemplate.dropCollection("task").awaitFirstOrNull()
        mongoTemplate.dropCollection(Item::class.java).awaitFirstOrNull()
        mongoTemplate.dropCollection(Ownership::class.java).awaitFirstOrNull()
    }

}