package com.rarible.protocol.order.listener.service.order

import com.rarible.core.apm.withTransaction
import com.rarible.core.daemon.job.JobHandler
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.dto.offchainEventMark
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import com.rarible.protocol.order.core.repository.order.OrderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
@ExperimentalCoroutinesApi
class OrderStartEndCheckerHandler(
    private val orderRepository: OrderRepository,
    private val orderDtoConverter: OrderDtoConverter,
    private val publisher: ProtocolOrderPublisher,
    private val orderIndexerProperties: OrderIndexerProperties,
    private val orderExpiredMetric: RegisteredCounter,
    private val orderStartedMetric: RegisteredCounter
) : JobHandler {
    private val logger: Logger = LoggerFactory.getLogger(OrderStartEndCheckerHandler::class.java)

    override suspend fun handle() {
        withTransaction("order_status") {
            update(Instant.now())
        }
    }

    internal suspend fun update(now: Instant) {
        logger.info("Starting to update status for orders...")
        merge(
            orderRepository.findExpiredOrders(now),
            orderRepository.findNotStartedOrders(now)
        )
            .filter { order -> order.isNoLegacyOpenSea() }
            .collect { order ->
                val saved = orderRepository.save(
                    order
                        .cancelEndedBid()
                        .withUpdatedStatus(now)
                )
                if (order.isEnded()) orderExpiredMetric.increment() else orderStartedMetric.increment()
                logger.info("Change order ${saved.id} status to ${saved.status}")
                val updateEvent = OrderUpdateEventDto(
                    eventId = UUID.randomUUID().toString(),
                    orderId = saved.id.toString(),
                    order = orderDtoConverter.convert(saved),
                    eventTimeMarks = offchainEventMark("indexer-out_order")
                )
                publisher.publish(updateEvent)
            }
    }

    private fun Order.isNoLegacyOpenSea(): Boolean {
        return this.isLegacyOpenSea(orderIndexerProperties.exchangeContractAddresses.openSeaV1).not()
    }
}
