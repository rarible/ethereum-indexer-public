package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

// RPN-1135: add pre-migration of OrderVersions.
@ChangeLog(order = "000012")
class ChangeLog00012AddAllFieldsFromOrderToOrderVersion {

    @ChangeSet(id = "ChangeLog00012AddAllFieldsFromOrderToOrderVersion.addAllFieldsFromOrderToOrderVersion", order = "1", author = "protocol")
    fun addAllFieldsFromOrderToOrderVersion(
        @NonLockGuarded template: ReactiveMongoTemplate,
        @NonLockGuarded orderVersionRepository: OrderVersionRepository
    ) = runBlocking {
        val logger = LoggerFactory.getLogger(javaClass)
        val orderRepository = MongoOrderRepository(template)

        logger.info("--- Start adding all fields from Order to OrderVersion")
        var counter = 0L

        orderRepository.findAll().collect { order ->

            try {
                orderVersionRepository.findAllByHash(order.hash).collect { version ->
                    orderVersionRepository.save(
                        version.copy(
                            type = order.type,
                            salt = order.salt,
                            start = version.start ?: order.start,
                            end = version.end ?: order.end,
                            data = order.data,
                            signature = version.signature ?: order.signature,
                            platform = order.platform
                        )
                    ).awaitFirst()
                }
                if (counter % 5000L == 0L) {
                    logger.info("Fixed $counter orders")
                }
                counter++
            } catch (ex: Exception) {
                logger.error("Can't fix order ${order.hash}")
            }
        }
        logger.info("--- All $counter order versions were updated")
    }
}
