package com.rarible.protocol.order.listener.job

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@Profile("!integration")
class OrderStartEndCheckerJob(
    private val properties: OrderListenerProperties,
    reactiveMongoTemplate: ReactiveMongoTemplate
) {
    private val logger: Logger = LoggerFactory.getLogger(OrderStartEndCheckerJob::class.java)
    private val orderRepository = MongoOrderRepository(reactiveMongoTemplate)

    @Scheduled(initialDelay = 60000, fixedDelayString = "\${listener.updateStatusByStartEndRate}")
    @CaptureTransaction(value = "order_status")
    fun update() = runBlocking {
        if (properties.updateStatusByStartEndEnabled.not()) return@runBlocking
        update(Instant.now())
    }

    suspend fun update(now: Instant) {
        logger.info("Starting to update status for orders...")
        var expired = 0L
        var alive = 0L

        merge(
            orderRepository.findExpiredOrders(now),
            orderRepository.findNotStartedOrders(now)
        ).collect { order ->
            if (order.isEnded()) {
                expired++
            } else {
                alive++
            }
            orderRepository.save(order.withUpdatedStatus())
            val all = alive + expired
            if (all % 10000L == 0L) {
                logger.info("Fixed $all orders")
            }
        }

        logger.info("Successfully finished updating order status: $expired expired, $alive alive")
    }
}
