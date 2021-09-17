package com.rarible.protocol.order.listener.job

import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("!integration")
class OrderResetMakeStockJob(
    private val properties: OrderListenerProperties,
    reactiveMongoTemplate: ReactiveMongoTemplate
) {
    private val logger: Logger = LoggerFactory.getLogger(OrderResetMakeStockJob::class.java)
    private val orderRepository = MongoOrderRepository(reactiveMongoTemplate)

    @Scheduled(initialDelay = 60000, fixedDelayString = "\${listener.resetMakeStockScheduleRate}")
    fun resetMakeStock() = runBlocking {
        if (properties.resetMakeStockEnabled.not()) return@runBlocking

        logger.info("Starting resetMakeStock()...")
        orderRepository.findActive().collect {
            orderRepository.save(it.withMakeStock())
        }
        logger.info("Successfully finished resetMakeStock().")
    }
}
