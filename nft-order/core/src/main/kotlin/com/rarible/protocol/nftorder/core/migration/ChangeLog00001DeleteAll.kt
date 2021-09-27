package com.rarible.protocol.nftorder.core.migration

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.Ownership
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "00004")
class ChangeLog00001DeleteAll() {

    @ChangeSet(
        id = "ChangeLog00004DeleteAll.deleteAll",
        order = "4",
        author = "protocol",
        runAlways = false
    )
    fun deleteAll(@NonLockGuarded mongoTemplate: ReactiveMongoTemplate) = runBlocking<Unit> {
        mongoTemplate.dropCollection("task")
        mongoTemplate.dropCollection(Item::class.java)
        mongoTemplate.dropCollection(Ownership::class.java)
    }

}