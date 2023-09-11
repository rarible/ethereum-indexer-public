package com.rarible.protocol.order.listener.service.order

import com.rarible.core.apm.withTransaction
import com.rarible.core.daemon.job.JobHandler
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.event.OrderListener
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.listener.configuration.StartEndWorkerProperties
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.merge
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@ExperimentalCoroutinesApi
class OrderStartEndCheckerHandler(
    private val orderRepository: OrderRepository,
    private val orderListener: OrderListener,
    private val properties: StartEndWorkerProperties,
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
        /**
         * We have to cancel order a bit in advance, as during order execution (witch can take time),
         * order can be already expired
         */
        val expiredNow = now + properties.cancelOffset

        merge(
            orderRepository.findExpiredOrders(expiredNow),
            orderRepository.findNotStartedOrders(now)
        )
            .collect { order ->
                val eventTimeMarks = orderOffchainEventMarks()
                val isExpired = order.isEndedAt(expiredNow)
                val saved = orderRepository.save(
                    order
                        .withAdvanceExpired(isExpired)
                        .cancelEndedBid()
                        .withUpdatedStatus(now)
                )
                if (isExpired) orderExpiredMetric.increment() else orderStartedMetric.increment()
                logger.info("Change order ${saved.id} status from ${order.status} to ${saved.status}")
                orderListener.onOrder(saved, eventTimeMarks, false)
            }
    }
}
