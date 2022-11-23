package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@ChangeLog(order = "00023")
class ChangeLog00023RemoveUnusedIndexes {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ChangeSet(
        id = "ChangeLog00023RemoveUnusedIndexes",
        order = "1",
        author = "protocol"
    )
    fun removeIndexes(@NonLockGuarded template: ReactiveMongoOperations) = runBlocking<Unit> {

        try {
            template.indexOps(MongoOrderRepository.COLLECTION)
                .dropIndex("createdAt_1__id_1").awaitFirst()
        } catch (ex: Exception) {
            logger.error("Drop index failed createdAt_1__id_1", ex)
        }
    }
}
