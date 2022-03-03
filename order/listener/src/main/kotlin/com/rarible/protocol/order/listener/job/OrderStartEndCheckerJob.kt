package com.rarible.protocol.order.listener.job

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@Profile("!integration")
class OrderStartEndCheckerJob(
    reactiveMongoTemplate: ReactiveMongoTemplate,
    private val properties: OrderListenerProperties,
    private val orderDtoConverter: OrderDtoConverter,
    private val publisher: ProtocolOrderPublisher,
    private val orderIndexerProperties: OrderIndexerProperties,
    meterRegistry: MeterRegistry
) {
    private val logger: Logger = LoggerFactory.getLogger(OrderStartEndCheckerJob::class.java)
    private val orderRepository = MongoOrderRepository(reactiveMongoTemplate)
    private val counter = meterRegistry.counter(properties.metricJobStartEnd)

    @Scheduled(initialDelay = 60000, fixedDelayString = "\${listener.updateStatusByStartEndRate}")
    @CaptureTransaction(value = "order_status")
    fun update() = runBlocking {
        if (properties.updateStatusByStartEndEnabled.not()) return@runBlocking
        update(Instant.now())
        counter.increment()
    }

    suspend fun update(now: Instant) {
        logger.info("Starting to update status for orders...")
        var expired = 0L
        var alive = 0L

        merge(
            orderRepository.findExpiredOrders(now),
            orderRepository.findNotStartedOrders(now)
        )
            .filter { order -> order.isNoLegacyOpenSea() }
            .collect { order ->
            if (order.isEnded()) {
                expired++
            } else {
                alive++
            }
            val saved = orderRepository.save(order.withUpdatedStatus())
            logger.info("Change order ${saved.hash} status to ${saved.status}")
            val updateEvent = OrderUpdateEventDto(
                eventId = UUID.randomUUID().toString(),
                orderId = saved.hash.toString(),
                order = orderDtoConverter.convert(saved)
            )
            publisher.publish(updateEvent)
            val all = alive + expired
            if (all % 10000L == 0L) {
                logger.info("Fixed $all orders")
            }
        }

        logger.info("Successfully finished updating order status: $expired expired, $alive alive")
    }


    private fun Order.isNoLegacyOpenSea(): Boolean {
        return this.isLegacyOpenSea(orderIndexerProperties.exchangeContractAddresses.openSeaV1).not()
    }
}
