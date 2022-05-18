package com.rarible.protocol.order.listener.job

import com.rarible.core.apm.CaptureTransaction
import com.rarible.core.apm.withTransaction
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.model.OpenSeaFetchState
import com.rarible.protocol.order.core.repository.opensea.OpenSeaFetchStateRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.OpenSeaOrdersLoadPeriodWorkerProperties
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderConverter
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderService
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderValidator
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.Instant

@CaptureTransaction(value = "opensea")
open class OpenSeaOrdersPeriodFetcherWorker(
    openSeaOrderService: OpenSeaOrderService,
    openSeaOrderConverter: OpenSeaOrderConverter,
    openSeaOrderValidator: OpenSeaOrderValidator,
    orderRepository: OrderRepository,
    orderUpdateService: OrderUpdateService,
    private val openSeaFetchStateRepository: OpenSeaFetchStateRepository,
    private val properties: OpenSeaOrdersLoadPeriodWorkerProperties,
    openSeaOrderSaveCounter: RegisteredCounter,
    meterRegistry: MeterRegistry,
) : OpenSeaOrdersFetcherWorker(
    openSeaOrderService = openSeaOrderService,
    openSeaFetchStateRepository = openSeaFetchStateRepository,
    openSeaOrderConverter = openSeaOrderConverter,
    openSeaOrderValidator = openSeaOrderValidator,
    orderRepository = orderRepository,
    orderUpdateService = orderUpdateService,
    properties = properties,
    saveCounter = openSeaOrderSaveCounter,
    meterRegistry = meterRegistry
) {
    override suspend fun handle() {
        try {
            withTransaction(name = "loadOpenSeaOrdersPeriod") {
                if (properties.enabled) {
                    val start = properties.start
                    val end =  properties.end

                    val stateId = getStateId(start = start, end = end)
                    val state = openSeaFetchStateRepository.get(stateId) ?: OpenSeaFetchState(start.epochSecond, stateId)
                    if (end.epochSecond > state.listedAfter) {
                        val newState = loadOpenSeaOrders(
                            state = state,
                            timeBoundary = end.epochSecond
                        )
                        openSeaFetchStateRepository.save(newState)
                    } else {
                        logger.info("[$logPrefix] All order was loaded from $state to $end")
                        delay(Duration.ofDays(1))
                    }
                }
            }
        } catch (ex: AssertionError) {
            throw IllegalStateException(ex)
        }
    }

    private fun getStateId(start: Instant, end: Instant): String {
        return "${STATE_ID_PREFIX}_from_${start.epochSecond}_to_${end.epochSecond}"
    }

    private companion object {
        const val STATE_ID_PREFIX = "open_sea_past_order_fetch"
    }
}
