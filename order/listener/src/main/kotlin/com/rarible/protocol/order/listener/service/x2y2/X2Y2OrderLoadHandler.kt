package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.core.apm.withSpan
import com.rarible.core.daemon.job.JobHandler
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.X2Y2FetchState
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.x2y2.X2Y2FetchStateRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.X2Y2OrdersLoadWorkerProperties
import com.rarible.x2y2.client.X2Y2ApiClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.time.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class X2Y2OrderLoadHandler(
    private val stateRepository: X2Y2FetchStateRepository,
    private val x2Y2ApiClient: X2Y2ApiClient,
    private val x2Y2OrderConverter: X2Y2OrderConverter,
    private val orderRepository: OrderRepository,
    private val x2y2OrderSaveCounter: RegisteredCounter,
    private val x2y2OrderLoadErrorCounter: RegisteredCounter,
    private val orderUpdateService: OrderUpdateService,
    private val properties: X2Y2OrdersLoadWorkerProperties
): JobHandler {

    private val logger: Logger = LoggerFactory.getLogger(X2Y2OrderLoadHandler::class.java)

    override suspend fun handle() {
        val state = stateRepository.byIdOrEmpty(X2Y2FetchState.ID)
        try {
            val result = withSpan(name = "fetchX2Y2Orders", labels = listOf("state" to state)) {
                x2Y2ApiClient.orders(cursor = state.cursor)
            }

            if (result.success && result.data.isNotEmpty()) {
                logger.info("Fetched ${result.data.size} x2y2 orders. Prepare to save")
                withSpan(name = "saveOrders", labels = listOf("size" to result.data.size)) {
                    val converted = result.data.map {
                        x2Y2OrderConverter.convertOrder(it)
                    }.filter {
                        orderRepository.findById(it.hash) == null
                    }

                    saveOrders(converted)
                }
                stateRepository.save(state.copy(
                    cursor = result.next,
                    lastError = null
                ))
            } else {
                logger.info("No orders fetched from x2y2 API")
                delay(properties.pollingPeriod)
            }
        } catch (e: Exception) {
            logger.warn("Unable to load x2y2 orders! ${e.message}")
            x2y2OrderLoadErrorCounter.increment()
            stateRepository.save(
                state.copy(
                    lastError = e.stackTraceToString()
                )
            )
            throw e
        }

    }

    private suspend fun saveOrders(converted: List<OrderVersion>) {
        converted.map {
            coroutineScope {
                async {
                    orderUpdateService.save(it)
                    x2y2OrderSaveCounter.increment()
                    logger.info("Saved x2y2 order: ${it.hash}")
                }
            }
        }.awaitAll()
    }
}
