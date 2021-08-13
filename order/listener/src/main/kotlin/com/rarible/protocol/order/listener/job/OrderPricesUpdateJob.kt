package com.rarible.protocol.order.listener.job

import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderUsdValue
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("!integration")
class OrderPricesUpdateJob(
    private val properties: OrderListenerProperties,
    private val priceUpdateService: PriceUpdateService,
    private val orderVersionRepository: OrderVersionRepository,
    reactiveMongoTemplate: ReactiveMongoTemplate
) {
    private val logger: Logger = LoggerFactory.getLogger(OrderPricesUpdateJob::class.java)
    private val orderRepository = MongoOrderRepository(reactiveMongoTemplate)

    @Scheduled(initialDelay = 60000, fixedDelayString = "\${listener.priceUpdateScheduleRate}")
    fun updateOrdersPrices() = runBlocking {
        if (properties.priceUpdateEnabled.not()) return@runBlocking

        logger.info("Starting updateOrdersPrices()...")

        orderRepository.findActive().collect {
            val usdValue = updateOrder(it)
            if (usdValue != null) {
                updateOrderVersions(it.hash, usdValue)
            }
        }
        logger.info("Successfully updated order prices.")
    }

    protected suspend fun updateOrder(order: Order): OrderUsdValue? {
        val usdValue = priceUpdateService.getAssetsUsdValue(
            make = order.make,
            take = order.take,
            at = nowMillis()
        ) ?: return null

        try {
            order.let { orderRepository.save(it.withOrderUsdValue(usdValue)) }
        } catch (_: OptimisticLockingFailureException) {
            optimisticLock {
                orderRepository.findById(order.hash)?.let { orderRepository.save(it.withOrderUsdValue(usdValue)) }
            }
        }
        return usdValue
    }

    protected suspend fun updateOrderVersions(hash: Word, usdValue: OrderUsdValue) {
        orderVersionRepository.findAllByHash(hash).collect { orderVersion ->
            try {
                orderVersion
                    .withOrderUsdValue(usdValue)
                    .let { orderVersionRepository.save(it).awaitFirst() }
            } catch (ex: Exception) {
                logger.error("Can't update prices for order version ${orderVersion.id}", ex)
            }
        }
    }
}
