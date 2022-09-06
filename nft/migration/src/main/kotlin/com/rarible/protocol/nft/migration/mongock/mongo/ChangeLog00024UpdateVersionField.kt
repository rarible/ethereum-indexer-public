package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.core.logging.Logger
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.Ownership
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

@ChangeLog(order = "00024")
class ChangeLog00024UpdateVersionField {
    @ChangeSet(
        id = "ChangeLog00024UpdateVersionField",
        order = "1",
        author = "protocol"
    )
    fun updateVersion(@NonLockGuarded template: ReactiveMongoOperations) = runBlocking<Unit> {

        listOf(
            Item.COLLECTION,
            Ownership.COLLECTION
        ).forEach {

            val updateResult = template.updateMulti(
                Query(Criteria("version").exists(false)),
                Update().set("version", 0),
                it
            ).awaitSingle()

            logger.info("Updated version field for $it {}/{}", updateResult.modifiedCount, updateResult.matchedCount)
        }
    }

    companion object {
        val logger by Logger()
    }
}