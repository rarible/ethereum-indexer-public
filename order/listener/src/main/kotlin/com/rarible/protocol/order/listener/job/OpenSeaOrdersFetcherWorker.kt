package com.rarible.protocol.order.listener.job

import com.rarible.core.apm.CaptureTransaction
import com.rarible.core.apm.withSpan
import com.rarible.core.apm.withTransaction
import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.model.OpenSeaFetchState
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.opensea.OpenSeaFetchStateRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.BaseOpenSeaOrderLoadWorkerProperties
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderConverter
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderService
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderValidator
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.time.delay
import org.springframework.boot.actuate.health.Health
import java.time.Duration
import java.time.Instant
import kotlin.math.min

@CaptureTransaction(value = "opensea")
open class OpenSeaOrdersFetcherWorker(
    private val openSeaOrderService: OpenSeaOrderService,
    private val openSeaFetchStateRepository: OpenSeaFetchStateRepository,
    private val openSeaOrderConverter: OpenSeaOrderConverter,
    private val openSeaOrderValidator: OpenSeaOrderValidator,
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val saveCounter : RegisteredCounter,
    private val properties: BaseOpenSeaOrderLoadWorkerProperties,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry,
    DaemonWorkerProperties().copy(pollingPeriod = properties.pollingPeriod, errorDelay = properties.errorDelay),
    properties.workerName)
{
    protected val logPrefix = properties.logPrefix

    init {
        logger.info("[$logPrefix] Start OpenSea loader with properties: $properties")
    }

    override suspend fun handle() {
        try {
            withTransaction(name = "loadOpenSeaOrders") {
                if (properties.enabled) {
                    val state = openSeaFetchStateRepository.get(properties.stateId) ?: getInitFetchState()
                    val now = nowMillis().epochSecond - properties.delay.seconds
                    val newState = loadOpenSeaOrders(
                        state = state,
                        timeBoundary = now
                    )
                    openSeaFetchStateRepository.save(newState)
                }
            }
        } catch (ex: AssertionError) {
            throw IllegalStateException(ex)
        }
    }

    protected suspend fun loadOpenSeaOrders(
        state: OpenSeaFetchState,
        timeBoundary: Long
    ): OpenSeaFetchState {

        val listedAfter = state.listedAfter
        val listedBefore = min(state.listedAfter + MAX_LOAD_PERIOD.seconds, timeBoundary)

        logger.info("[$logPrefix] Starting fetching OpenSea orders, listedAfter=$listedAfter, listedBefore=$listedBefore")
        val openSeaOrders = withSpan(
            name = "fetchOpenSeaOrders",
            labels = listOf("listedAfter" to listedAfter, "listedBefore" to listedBefore)
        ) {
            openSeaOrderService.getNextOrdersBatch(
                listedAfter = listedAfter,
                listedBefore = listedBefore,
                loadPeriod = properties.loadPeriod,
                logPrefix = properties.logPrefix)
        }
        if (openSeaOrders.isNotEmpty()) {
            val ids = openSeaOrders.map { it.id }
            val minId = ids.minOrNull() ?: error("Can't be empty value")
            val maxId = ids.maxOrNull() ?: error("Can't be empty value")

            val createdAts = openSeaOrders.map { it.createdAt }
            val minCreatedAt = createdAts.minOrNull() ?: error("Can't be empty value")
            val maxCreatedAt = createdAts.maxOrNull() ?: error("Can't be empty value")

            logger.info("[$logPrefix] Fetched ${openSeaOrders.size}, minId=$minId, maxId=$maxId, minCreatedAt=$minCreatedAt, maxCreatedAt=$maxCreatedAt, new OpenSea orders: ${openSeaOrders.joinToString { it.orderHash.toString() }}")

            coroutineScope {
                withSpan(name = "saveOpenSeaOrders", labels = listOf("size" to openSeaOrders.size)) {
                    openSeaOrders
                        .chunked(properties.saveBatchSize)
                        .map { chunk ->
                            chunk.map { openSeaOrder ->
                                async {
                                    val version = openSeaOrderConverter
                                        .convert(openSeaOrder)
                                        ?.takeIf { openSeaOrderValidator.validate(it) }

                                    if (version != null) {
                                        saveOrder(version)
                                    }
                                    openSeaOrder.id
                                }
                            }.awaitAll()
                        }
                        .flatten()
                        .lastOrNull()
                }
            }
            logger.info("[$logPrefix] All new OpenSea orders saved")
        } else {
            logger.info("[$logPrefix] No new orders to fetch")
            delay(pollingPeriod)
        }
        val nextListedAfter = if (listedBefore > timeBoundary) timeBoundary else listedBefore
        return state.withListedAfter(nextListedAfter)
    }

    private suspend fun saveOrder(orderVersion: OrderVersion) {
        if (orderRepository.findById(orderVersion.hash) == null) {
            orderUpdateService.save(orderVersion)
            saveCounter.increment()
            logger.info("[$logPrefix] Saved new OpenSea order ${orderVersion.hash}")
        }
    }

    private fun getInitFetchState(): OpenSeaFetchState {
        return OpenSeaFetchState((Instant.now() - properties.delay - properties.loadPeriod).epochSecond)
    }

    private companion object {
        val MAX_LOAD_PERIOD: Duration = Duration.ofSeconds(30)
    }

    override fun health(): Health {
        return Health.up().build()
    }
}
