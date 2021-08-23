package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.order.core.model.Platform
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

@ChangeLog(order = "000010")
class ChangeLog00010AddPlatformFieldToOrder {

    @ChangeSet(id = "ChangeLog00009AddPlatformFieldToOrder.addPlatformFieldToOrder", order = "1", author = "protocol")
    fun removeOrdersWithMakeValueZero(
        @NonLockGuarded template: ReactiveMongoTemplate,
        @NonLockGuarded orderVersionRepository: OrderVersionRepository
    ) = runBlocking {
        val logger = LoggerFactory.getLogger(javaClass)
        val orderRepository = MongoOrderRepository(template)

        logger.info("--- Start add order platform value")
        var counter = 0L

        orderRepository.findAll().collect { order ->
            try {
                try {
                    template.save(order.copy(platform = Platform.RARIBLE)).awaitFirst()
                } catch (_: OptimisticLockingFailureException) {
                    optimisticLock {
                        orderRepository.findById(order.hash)
                            ?.let { template.save(it.copy(platform = Platform.RARIBLE)).awaitFirst() }
                    }
                }
                orderVersionRepository.findAllByHash(order.hash).collect { version ->
                    try {
                        orderVersionRepository.save(version.copy(platform = Platform.RARIBLE)).awaitFirst()
                    } catch (_: OptimisticLockingFailureException) {
                        optimisticLock {
                            orderVersionRepository.findById(version.id).awaitFirstOrNull()
                                ?.let {
                                    orderVersionRepository.save(it.copy(platform = Platform.RARIBLE))
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
        logger.info("--- All $counter orders were updated")
    }
}
