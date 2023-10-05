package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@ChangeLog(order = "00027")
class ChangeLog00027RemoveUnusedCollections {

    private val unusedCollections = listOf(
        "collection_stat"
    )

    @ChangeSet(id = "ChangeLog00027RemoveUnusedCollections", order = "1", author = "protocol")
    fun removeIndexes(@NonLockGuarded template: ReactiveMongoOperations) = runBlocking<Unit> {
        unusedCollections.forEach {
            template.dropCollection(it).awaitFirstOrNull()
        }
    }
}
