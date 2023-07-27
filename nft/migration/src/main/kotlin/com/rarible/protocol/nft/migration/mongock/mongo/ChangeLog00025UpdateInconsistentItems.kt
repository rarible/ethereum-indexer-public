package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.repository.InconsistentItemRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant
import kotlin.random.Random

@ChangeLog(order = "00025")
class ChangeLog00025UpdateInconsistentItems {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ChangeSet(
        id = "ChangeLog00025UpdateInconsistentItems",
        order = "1",
        author = "protocol"
    )
    fun updateInconsistentItems(@NonLockGuarded repository: InconsistentItemRepository) {
        updateInconsistentItemsInner(repository, 1000)
    }

    fun updateInconsistentItemsInner(
        repository: InconsistentItemRepository,
        batchSize: Int = 1000,
    ) = runBlocking<Unit> {
        val random = Random(Instant.now().toEpochMilli())

        val query = Query.query(Criteria("lastUpdatedAt").`is`(null))
            .limit(batchSize)
        do {
            val result = repository.search(query)
            logger.info("Got ${result.size} inconsistent items to update")
            result.forEach {
                val updated = it.copy(
                    lastUpdatedAt = Instant.ofEpochMilli(1609448400000 + random.nextLong(86400000 * 365L)) // random point in 2021
                )
                repository.save(updated)
                logger.info("Updated inconsistentItem: $updated")
            }
        } while (result.isNotEmpty())

        repository.createIndexes()
    }
}
