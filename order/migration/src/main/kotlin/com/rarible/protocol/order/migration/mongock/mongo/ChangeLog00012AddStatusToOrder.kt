package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "000012")
class ChangeLog00012AddStatusToOrder {

    @ChangeSet(id = "ChangeLog00012AddStatusToOrder.migrate", order = "1", author = "protocol")
    fun migrate(
        @NonLockGuarded template: ReactiveMongoTemplate
    ) = runBlocking {
        val logger = LoggerFactory.getLogger(javaClass)
        val orderRepository = MongoOrderRepository(template)

        var counter = 0L
        orderRepository.findAll().collect { order ->
            try {
                orderRepository.save(order.withCurrentStatus())
                if (counter % 10000L == 0L) {
                    logger.info("Fixed $counter orders")
                }
                counter++
            } catch (ex: Exception) {
                logger.error("Failed to set status for ${order.hash} order")
            }
        }
        logger.info("--- Status was set for $counter orders ")
    }
}
