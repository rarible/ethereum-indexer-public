package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.service.OrderUpdateService
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo

@ChangeLog(order = "000014")
class ChangeLog00014RecalculateMakeStock {

    @ChangeSet(id = "ChangeLog00014RecalculateMakeStock.orders", order = "1", author = "protocol")
    fun orders(
        @NonLockGuarded template: ReactiveMongoTemplate,
        @NonLockGuarded orderUpdateService: OrderUpdateService
    ) = runBlocking {
        val logger = LoggerFactory.getLogger(javaClass)
        logger.info("Starting to update makeStock for orders...")

        var nullableStock = 0L
        var nonZeroStock = 0L
        var changed = 0L
        val query = Query(Criteria().andOperator(Order::status isEqualTo OrderStatus.INACTIVE))

        template.query<Order>().matching(query).all().asFlow().collect { order ->
            try {
                val updated = orderUpdateService.updateMakeStock(order.hash)
                if (updated?.makeStock != EthUInt256.ZERO) {
                    nonZeroStock++
                } else {
                    nullableStock++
                }
                if (updated != null && updated.makeStock != order.makeStock) {
                    changed++
                }
                val all = nonZeroStock + nullableStock
                if (all % 10000L == 0L) {
                    logger.info("Fixed $all orders")
                }
            } catch (ex: Exception) {
                logger.error("Failed to update makeStock ${order.hash} order")
            }
        }
        logger.info("MakeStock was updated for orders: makeStock == 0 for $nullableStock orders, makeStock > 0 for $nonZeroStock orders, changed $changed orders.")
    }
}
