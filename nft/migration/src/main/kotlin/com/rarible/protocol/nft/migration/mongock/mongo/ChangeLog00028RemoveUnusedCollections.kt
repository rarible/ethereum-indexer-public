package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@ChangeLog(order = "00028")
class ChangeLog00028RemoveUnusedCollections {

    private val unusedCollections = listOf(
        "cache-item-meta",
        "cache-loader-task-ids",
        "cache_hashmasks",
        "cache_hegic",
        "cache_loot",
        "cache_meta",
        "cache_opensea",
        "cache_properties",
        "cache_waifusion",
        "cache_yinsure",
        "initializeStatus", // TODO maybe need it?
        "item_metadata",
        "loader-tasks",
        "pending_item_token_uri", // TODO maybe needed it?
        "pending_log_item_properties", // TODO maybe needed it?
        "state", // TODO maybe needed it?
        "temporary_item_properties", // TODO maybe needed it?
        "token_metadata"

    )

    @ChangeSet(id = "ChangeLog00028RemoveUnusedCollections", order = "1", author = "protocol")
    fun removeIndexes(@NonLockGuarded template: ReactiveMongoOperations) = runBlocking<Unit> {
        unusedCollections.forEach {
            template.dropCollection(it).awaitFirstOrNull()
        }
    }
}
