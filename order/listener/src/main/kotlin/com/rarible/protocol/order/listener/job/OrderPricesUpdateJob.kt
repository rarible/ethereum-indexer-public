package com.rarible.protocol.order.listener.job

import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.repository.order.OrderRepository
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
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
@Profile("!integration")
class OrderPricesUpdateJob(
    private val properties: OrderListenerProperties,
    private val priceUpdateService: PriceUpdateService,
    private val orderRepository: OrderRepository,
    private val orderVersionRepository: OrderVersionRepository
) {
    @Scheduled(initialDelay = 60000, fixedDelayString = "\${listener.priceUpdateScheduleRate}")
    fun updateOrdersPrices() = runBlocking {
        if (properties.priceUpdateEnabled.not()) return@runBlocking

        logger.info("Starting updateOrdersPrices()...")

        orderRepository.findActive().collect {
            val updatedOrder = updateOrder(it)

            if (updatedOrder != null) {
                updateOrderVersions(updatedOrder.hash, updatedOrder.makePriceUsd, updatedOrder.takePriceUsd)
            }
        }
        logger.info("Successfully updated order prices.")
    }

    protected suspend fun updateOrder(order: Order): Order? {
        return try {
            order
                .let { priceUpdateService.updateOrderPrice(it, nowMillis()) }
                .let { orderRepository.save(it) }
        } catch (_: OptimisticLockingFailureException) {
            optimisticLock {
                orderRepository.findById(order.hash)
                    ?.let { priceUpdateService.updateOrderPrice(it, nowMillis()) }
                    ?.let { orderRepository.save(it) }
            }
        }
    }

    protected suspend fun updateOrderVersions(hash: Word, makePriceUsb: BigDecimal?, takePriceUsd: BigDecimal?) {
        orderVersionRepository.findAllByHash(hash).collect { orderVersion ->
            try {
                orderVersion
                    .withPricesUsd(makePriceUsb, takePriceUsd)
                    .let { orderVersionRepository.save(it).awaitFirst() }
            } catch (ex: Exception) {
                logger.error("Can't update prices for order version ${orderVersion.id}", ex)
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(OrderPricesUpdateJob::class.java)
    }
}
