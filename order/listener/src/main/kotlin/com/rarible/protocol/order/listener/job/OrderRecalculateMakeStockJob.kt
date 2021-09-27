package com.rarible.protocol.order.listener.job

import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirstOrNull
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
class OrderRecalculateMakeStockJob(
    private val properties: OrderListenerProperties,
    reactiveMongoTemplate: ReactiveMongoTemplate,
    private val orderUpdateService: OrderUpdateService
) {
    private val logger: Logger = LoggerFactory.getLogger(OrderRecalculateMakeStockJob::class.java)
    private val orderRepository = MongoOrderRepository(reactiveMongoTemplate)

    @Scheduled(initialDelay = 60000, fixedDelayString = "\${listener.resetMakeStockScheduleRate}")
    fun update() = runBlocking {
        if (properties.resetMakeStockEnabled.not()) return@runBlocking

        logger.info("Starting to update makeStock for orders...")

        val result = orderRepository.resetMakeStockAfter(Instant.now()).awaitFirstOrNull()
        logger.info("Successfully reset makeStock for ${result?.modifiedCount} documents")

        orderRepository.findActualZeroMakeStock(Instant.now()).collect {
            orderUpdateService.updateMakeStock(hash = it.hash)
        }

        logger.info("Successfully finished update makeStock for orders.")
    }
}
