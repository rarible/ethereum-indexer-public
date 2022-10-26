package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.core.logging.Logger
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@ChangeLog(order = "00022")
class ChangeLog00022RemoveUnusedIndexes {
    @ChangeSet(
        id = "ChangeLog00022RemoveUnusedIndexes",
        order = "1",
        author = "protocol"
    )
    fun removeIndexes(@NonLockGuarded template: ReactiveMongoOperations) = runBlocking<Unit> {

        template.indexOps(MongoOrderRepository.COLLECTION)
            .dropIndex("maker_1_status_1_make.type.token_1").awaitFirst()

        template.indexOps(MongoOrderRepository.COLLECTION)
            .dropIndex("platform_1_maker_1_data.nonce_1").awaitFirst()

     template.indexOps(MongoOrderRepository.COLLECTION)
            .dropIndex("status_1_maker_1_data.counter_1").awaitFirst()

     template.indexOps(MongoOrderRepository.COLLECTION)
            .dropIndex("make.type.nft_1_lastUpdateAt_1__id_1").awaitFirst()

    }

    companion object {
        val logger by Logger()
    }
}
