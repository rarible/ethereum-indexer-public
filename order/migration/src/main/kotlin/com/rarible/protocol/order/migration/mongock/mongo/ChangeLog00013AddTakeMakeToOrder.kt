package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import java.math.BigDecimal

@ChangeLog(order = "000013")
class ChangeLog00013AddTakeMakeToOrder {

    @ChangeSet(id = "ChangeLog00013AddTakeMakeToOrder.orders", order = "1", author = "protocol")
    fun orders(
        @NonLockGuarded template: ReactiveMongoTemplate
    ) = runBlocking {
        val logger = LoggerFactory.getLogger(javaClass)

        var counter = 0L
        var query = Query().with(Sort.by(Sort.Direction.DESC, Order::lastUpdateAt.name))
        template.find<Order>(query, MongoOrderRepository.COLLECTION).asFlow().collect { order ->
            try {
                if (order.make.type.nft) {
                    val updated = order.copy(takePrice = BigDecimal.valueOf(order.take.value.value.toLong()))
                    template.save(updated, MongoOrderRepository.COLLECTION).awaitFirstOrNull()
                    counter++
                } else if (order.take.type.nft) {
                    val updated = order.copy(makePrice = BigDecimal.valueOf(order.make.value.value.toLong()))
                    template.save(updated, MongoOrderRepository.COLLECTION).awaitFirstOrNull()
                    counter++
                }
                if (counter % 10000L == 0L) {
                    logger.info("Fixed $counter orders")
                }
            } catch (ex: Exception) {
                logger.error("Failed to set price for ${order.hash} order")
            }
        }
        logger.info("--- Prices were set for $counter orders ")
    }

    @ChangeSet(id = "ChangeLog00013AddTakeMakeToOrder.orderVersions", order = "1", author = "protocol")
    fun orderVersions(
        @NonLockGuarded template: ReactiveMongoTemplate
    ) = runBlocking {
        val logger = LoggerFactory.getLogger(javaClass)

        var counter = 0L
        var query = Query().with(Sort.by(Sort.Direction.DESC, OrderVersion::createdAt.name))
        template.find<Order>(query, OrderVersionRepository.COLLECTION).asFlow().collect { orderVersion ->
            try {
                if (orderVersion.make.type.nft) {
                    val updated = orderVersion.copy(takePrice = BigDecimal.valueOf(orderVersion.take.value.value.toLong()))
                    template.save(updated, MongoOrderRepository.COLLECTION).awaitFirstOrNull()
                    counter++
                } else if (orderVersion.take.type.nft) {
                    val updated = orderVersion.copy(makePrice = BigDecimal.valueOf(orderVersion.make.value.value.toLong()))
                    template.save(updated, MongoOrderRepository.COLLECTION).awaitFirstOrNull()
                    counter++
                }
                if (counter % 10000L == 0L) {
                    logger.info("Fixed $counter orders")
                }
            } catch (ex: Exception) {
                logger.error("Failed to set price for ${orderVersion.hash} order version")
            }
        }
        logger.info("--- Prices were set for $counter orders version ")
    }
}
