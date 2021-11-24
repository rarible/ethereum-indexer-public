package com.rarible.protocol.order.listener.job

import com.rarible.core.apm.CaptureTransaction
import com.rarible.core.common.nowMillis
import com.rarible.protocol.order.core.event.NftOrdersPriceUpdateListener
import com.rarible.protocol.order.core.model.OrderKind
import com.rarible.protocol.order.core.model.makeNftItemId
import com.rarible.protocol.order.core.model.takeNftItemId
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.service.order.OrderPriceUpdateService
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
class OrderPricesUpdateJob(
    private val properties: OrderListenerProperties,
    private val orderPriceUpdateService: OrderPriceUpdateService,
    private val nftOrdersPriceUpdateListener: NftOrdersPriceUpdateListener,
    reactiveMongoTemplate: ReactiveMongoTemplate
) {
    private val logger: Logger = LoggerFactory.getLogger(OrderPricesUpdateJob::class.java)
    private val orderRepository = MongoOrderRepository(reactiveMongoTemplate)

    @Scheduled(initialDelay = 60000, fixedDelayString = "\${listener.priceUpdateScheduleRate}")
    @CaptureTransaction(value = "order_price")
    fun updateOrdersPrices() = runBlocking {
        if (properties.priceUpdateEnabled.not()) return@runBlocking

        logger.info("Starting updateOrdersPrices()...")

        val updateTime = nowMillis()
        orderRepository.findActive().collect { order ->
            orderPriceUpdateService.updateOrderPrice(order.hash, updateTime)

            val makeNftItemId = order.makeNftItemId
            if (makeNftItemId != null) {
                nftOrdersPriceUpdateListener.onNftOrders(makeNftItemId, OrderKind.SELL, listOf(order))
            }

            val takeNftItemId = order.takeNftItemId
            if (takeNftItemId != null) {
                nftOrdersPriceUpdateListener.onNftOrders(takeNftItemId, OrderKind.BID, listOf(order))
            }
        }
        logger.info("Successfully updated order prices.")
    }
}
