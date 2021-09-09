package com.rarible.protocol.order.listener.job

import com.rarible.core.common.nowMillis
import com.rarible.protocol.order.core.model.OrderUsdValue
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("!integration")
class OrderPricesUpdateJob(
    private val properties: OrderListenerProperties,
    private val priceUpdateService: PriceUpdateService,
    private val orderReduceService: OrderReduceService,
    private val orderVersionRepository: OrderVersionRepository,
    reactiveMongoTemplate: ReactiveMongoTemplate
) {
    private val logger: Logger = LoggerFactory.getLogger(OrderPricesUpdateJob::class.java)
    private val orderRepository = MongoOrderRepository(reactiveMongoTemplate)

    @Scheduled(initialDelay = 60000, fixedDelayString = "\${listener.priceUpdateScheduleRate}")
    fun updateOrdersPrices() = runBlocking {
        if (properties.priceUpdateEnabled.not()) return@runBlocking

        logger.info("Starting updateOrdersPrices()...")

        val updateTime = nowMillis()
        orderRepository.findActive().collect {
            val usdValue = priceUpdateService.getAssetsUsdValue(
                make = it.make,
                take = it.take,
                at = updateTime
            ) ?: return@collect

            // TODO[RPN-824]: it is incorrect to set the same 'usdValue' for all OrderVersions because their make/take may differ.
            updateOrderVersions(it.hash, usdValue)
            try {
                orderReduceService.updateOrder(it.hash)
            } catch (e: Exception) {
                logger.error("Failed to update prices of order ${it.hash}", e)
            }
        }
        logger.info("Successfully updated order prices.")
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
