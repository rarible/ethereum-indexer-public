package com.rarible.protocol.order.listener.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.core.logging.addToMdc
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.ReservoirAsksEventFetchState
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import com.rarible.protocol.order.core.service.OrderCancelService
import com.rarible.protocol.order.listener.configuration.ReservoirProperties
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import com.rarible.reservoir.client.ReservoirClient
import com.rarible.reservoir.client.model.common.OrderStatus
import com.rarible.reservoir.client.model.v3.OrderEventsRequest
import com.rarible.reservoir.client.model.v3.ReservoirOrderEvent
import com.rarible.reservoir.client.model.v3.ReservoirOrderEvents
import io.daonomic.rpc.domain.Word
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.Instant

class ReservoirOrdersReconciliationWorker(
    private val reservoirClient: ReservoirClient,
    private val aggregatorStateRepository: AggregatorStateRepository,
    private val orderCancelService: OrderCancelService,
    private val properties: ReservoirProperties,
    private val orderRepository: OrderRepository,
    private val foreignOrderMetrics: ForeignOrderMetrics,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.pollingPeriod,
        errorDelay = properties.errorDelay
    ),
    workerName = "reservoir-orders-reconciliation-job"
) {
    public override suspend fun handle() {
        val response = loadNextBatch()
        handleResponse(response)
        recordState(response)
        recordDelay(response)
        waitIfNecessary(response)
    }

    private suspend fun loadNextBatch(): ReservoirOrderEvents {
        val state = aggregatorStateRepository.getReservoirAsksEventState()
        logger.info("Requesting reservoir ask order events from state: $state")
        return reservoirClient.getAskEventsV3(
            OrderEventsRequest(
                startTimestamp = null,
                sortDirection = OrderEventsRequest.SortDirection.ASC,
                continuation = state?.cursor,
                limit = properties.size,
            )
        ).ensureSuccess()
    }

    private suspend fun handleResponse(response: ReservoirOrderEvents) {
        response.events.forEach { event ->
            if (event.order.status in CANCEL_STATUSES) {
                handleCancelEvent(event)
            }
        }
    }

    private suspend fun recordState(response: ReservoirOrderEvents) {
        response.continuation?.run {
            aggregatorStateRepository.save(ReservoirAsksEventFetchState(cursor = this))
        }
    }

    private suspend fun waitIfNecessary(response: ReservoirOrderEvents) {
        if (response.events.size < properties.size) {
            logger.info(
                "Result size ${response.events.size} < ${properties.size}. Will wait for ${properties.pollingPeriod}"
            )
            delay(properties.pollingPeriod)
        }
    }

    private fun recordDelay(response: ReservoirOrderEvents) {
        if (response.events.isNotEmpty()) {
            meterRegistry.timer("reservoir_reconciliation_delay")
                .record(Duration.between(response.events.last().event.createdAt, Instant.now()))
        }
    }

    private suspend fun handleCancelEvent(event: ReservoirOrderEvent) {
        val eventTimeMarks = orderOffchainEventMarks()
        val id = Word.apply(event.order.id)
        val order = orderRepository.findById(id) ?: return
        if (order.status == com.rarible.protocol.order.core.model.OrderStatus.ACTIVE) {
            logger.warn(
                "Found canceled order but active in the database ${order.type}: $id. " +
                    "Will cancel: ${properties.cancelEnabled}"
            )
            foreignOrderMetrics.onOrderInconsistency(platform = order.platform, status = event.order.status.name)
            if (properties.cancelEnabled) {
                addToMdc("orderType" to order.type.name) {
                    logger.info("Unexpected order cancellation: ${order.type}:${order.hash}")
                }
                orderCancelService.cancelOrder(
                    id = id,
                    eventTimeMarksDto = eventTimeMarks,
                )
            }
        }
    }

    companion object {
        private val CANCEL_STATUSES = setOf(
            OrderStatus.CANCELED,
            OrderStatus.INACTIVE,
            OrderStatus.EXPIRED,
        )
    }
}
