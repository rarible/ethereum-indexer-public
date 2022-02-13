package com.rarible.protocol.order.listener.job

import com.rarible.core.apm.CaptureTransaction
import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.order.core.model.OpenSeaFetchState
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.opensea.OpenSeaFetchStateRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderConverter
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderService
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
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val properties: OrderListenerProperties,
    meterRegistry: MeterRegistry,
    workerProperties: DaemonWorkerProperties
) : SequentialDaemonWorker(meterRegistry, workerProperties, "open-sea-orders-fetcher-job") {

    override suspend fun handle() {
        try {
            handleSafely()
        } catch (ex: AssertionError) {
            throw IllegalStateException(ex)
        }
    }

    private suspend fun handleSafely() {
        if (properties.loadOpenSeaOrders.not()) return

        val state = openSeaFetchStateRepository.get() ?: INIT_FETCH_STATE
        val now = nowMillis().epochSecond - properties.loadOpenSeaDelay.seconds

        val listedAfter = state.listedAfter
        val listedBefore = min(state.listedAfter + MAX_LOAD_PERIOD.seconds, now)

        logger.info("[OpenSea] Starting fetching OpenSea orders, listedAfter=$listedAfter, listedBefore=$listedBefore")
        val openSeaOrders =
            openSeaOrderService.getNextOrdersBatch(listedAfter = listedAfter, listedBefore = listedBefore)

        if (openSeaOrders.isNotEmpty()) {
            val ids = openSeaOrders.map { it.id }
            val minId = ids.minOrNull() ?: error("Can't be empty value")
            val maxId = ids.maxOrNull() ?: error("Can't be empty value")

            val createdAts = openSeaOrders.map { it.createdAt }
            val minCreatedAt = createdAts.minOrNull() ?: error("Can't be empty value")
            val maxCreatedAt = createdAts.maxOrNull() ?: error("Can't be empty value")

            logger.info("[OpenSea] Fetched ${openSeaOrders.size}, minId=$minId, maxId=$maxId, minCreatedAt=$minCreatedAt, maxCreatedAt=$maxCreatedAt, new OpenSea orders: ${openSeaOrders.joinToString { it.orderHash.toString() }}")

            coroutineScope {
                openSeaOrders
                    .chunked(properties.saveOpenSeaOrdersBatchSize)
                    .map { chunk ->
                        chunk.map { openSeaOrder ->
                            async {
                                val version = openSeaOrderConverter.convert(openSeaOrder)
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
            logger.info("[OpenSea] All new OpenSea orders saved")
        } else {
            logger.info("[OpenSea] No new orders to fetch")
            delay(pollingPeriod)
        }
        val nextListedAfter = if (listedBefore > now) now else listedBefore
        openSeaFetchStateRepository.save(state.withListedAfter(nextListedAfter))
    }

    private suspend fun saveOrder(orderVersion: OrderVersion) {
        if (orderRepository.findById(orderVersion.hash) == null) {
            orderUpdateService.save(orderVersion)
        }
        logger.info("Saved new OpenSea order ${orderVersion.hash}")
    }

    private companion object {
        val MAX_LOAD_PERIOD: Duration = Duration.ofSeconds(30)
        val INIT_FETCH_STATE: OpenSeaFetchState = OpenSeaFetchState((Instant.now() - MAX_LOAD_PERIOD).epochSecond)
    }

    override fun health(): Health {
        return Health.up().build()
    }
}
