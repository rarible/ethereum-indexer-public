package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.PriceUpdateService
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
        @NonLockGuarded priceUpdateService: PriceUpdateService,
        @NonLockGuarded template: ReactiveMongoTemplate
    ) = runBlocking {
        val logger = LoggerFactory.getLogger(javaClass)

        var counter = 0L
        val query = Query().with(Sort.by(Sort.Direction.DESC, Order::lastUpdateAt.name))
        template.find<Order>(query, MongoOrderRepository.COLLECTION).asFlow().collect { order ->
            try {
                val updated = priceUpdateService.withUpdatedAllPrices(order)
                template.save(updated, MongoOrderRepository.COLLECTION).awaitFirstOrNull()
                counter++
                if (counter % 10000L == 0L) {
                    logger.info("Fixed $counter orders")
                }
            } catch (ex: Exception) {
                logger.error("Failed to set price for ${order.hash} order")
            }
        }
        logger.info("--- Prices were set for $counter orders ")
    }

    @ChangeSet(id = "ChangeLog00013AddTakeMakeToOrder.orderVersions", order = "2", author = "protocol")
    fun orderVersions(
        @NonLockGuarded priceUpdateService: PriceUpdateService,
        @NonLockGuarded template: ReactiveMongoTemplate
    ) = runBlocking {
        val logger = LoggerFactory.getLogger(javaClass)

        var counter = 0L
        var query = Query().with(Sort.by(Sort.Direction.DESC, OrderVersion::createdAt.name))
        template.find<OrderVersion>(query).asFlow().collect { orderVersion ->
            try {
                val updated = priceUpdateService.withUpdatedAllPrices(orderVersion)
                template.save(updated, OrderVersionRepository.COLLECTION).awaitFirstOrNull()
                counter++
                if (counter % 10000L == 0L) {
                    logger.info("Fixed $counter order versions")
                }
            } catch (ex: Exception) {
                logger.error("Failed to set price for ${orderVersion.hash} order versions")
            }
        }
        logger.info("--- Prices were set for $counter orders version ")
    }
}
