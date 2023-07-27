package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.repository.nonce.NonceHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "00025")
class ChangeLog00025DeleteManuallyCreatedIndices {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ChangeSet(
        id = "ChangeLog00025DeleteManuallyCreatedIndices.dropIndices",
        order = "1",
        author = "protocol",
        runAlways = false
    )
    fun dropIndices(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking {

        dropIndices(
            template, NonceHistoryRepository.COLLECTION,
            listOf("status_1_data.maker_1 ")
        )
    }

    private suspend fun dropIndices(
        template: ReactiveMongoTemplate,
        collection: String,
        indices: List<String>
    ) {
        val ops = template.indexOps(collection)
        indices.forEach {
            try {
                ops.dropIndex(it).awaitFirst()
            } catch (ex: Exception) {
                logger.error("Drop index '$it' failed for collection '$collection'", ex)
            }
        }
    }
}
