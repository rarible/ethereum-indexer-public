package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "000011")
class ChangeLog00011AddAllFieldsFromOrderToOrderVersion {

    @ChangeSet(id = "ChangeLog00011AddAllFieldsFromOrderToOrderVersion.addAllFieldsFromOrderToOrderVersion", order = "1", author = "protocol")
    fun addAllFieldsFromOrderToOrderVersion(
        @NonLockGuarded template: ReactiveMongoTemplate,
        @NonLockGuarded orderVersionRepository: OrderVersionRepository
    ) = runBlocking {
        val logger = LoggerFactory.getLogger(javaClass)
        val orderRepository = MongoOrderRepository(template)

        logger.info("--- Start adding all fields from Order to OrderVersion")
        var counter = 0L

        orderRepository.findAll().collect { order ->
            fun OrderVersion.addOrderFields() = copy(
                type = order.type,
                salt = order.salt,
                start = order.start,
                end = order.end,
                data = order.data,
                signature = order.signature,
                platform = order.platform
            )

            try {
                orderVersionRepository.findAllByHash(order.hash).collect { version ->
                    try {
                        orderVersionRepository.save(version.addOrderFields()).awaitFirst()
                    } catch (_: OptimisticLockingFailureException) {
                        optimisticLock {
                            orderVersionRepository.findById(version.id).awaitFirstOrNull()
                                ?.let {
                                    orderVersionRepository.save(it.addOrderFields()).awaitFirst()
                                }
                        }
                    }
                }
                if (counter % 50000L == 0L) {
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
