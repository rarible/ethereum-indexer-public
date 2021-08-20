package com.rarible.protocol.order.listener.job

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.order.core.event.OrderVersionListener
import com.rarible.protocol.order.core.model.OpenSeaFetchState
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.opensea.OpenSeaFetchStateRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderConverter
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderService
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.Instant
import kotlin.math.min

class OpenSeaOrdersFetcherWorker(
    private val openSeaOrderService: OpenSeaOrderService,
    private val openSeaFetchStateRepository: OpenSeaFetchStateRepository,
    private val openSeaOrderConverter: OpenSeaOrderConverter,
    private val orderRepository: OrderRepository,
    private val orderVersionRepository: OrderVersionRepository,
    private val properties: OrderListenerProperties,
    private val orderVersionListener: OrderVersionListener,
    meterRegistry: MeterRegistry,
    workerProperties: DaemonWorkerProperties
) : SequentialDaemonWorker(meterRegistry, workerProperties, "open-sea-orders-fetcher-job") {

    override suspend fun handle() {
        if (properties.loadOpenSeaOrders.not()) return

        val state = openSeaFetchStateRepository.get() ?: INIT_FETCH_STATE

        val now = nowMillis().epochSecond
        val listedAfter = state.listedAfter
        val listedBefore = min(state.listedAfter + MAX_LOAD_PERIOD.seconds, now)

        logger.info("[OpenSea] Starting fetching OpenSea orders, listedAfter=$listedAfter, listedBefore=$listedBefore")
        val openSeaOrders = openSeaOrderService.getNextOrdersBatch(listedAfter = listedAfter, listedBefore = listedBefore)

        val nextListedAfter = if (openSeaOrders.isNotEmpty()) {
            val ids = openSeaOrders.map { it.id }
            val minId = ids.min() ?: error("Can't be empty value")
            val maxId = ids.max() ?: error("Can't be empty value")

            val createdAts = openSeaOrders.map { it.createdAt }
            val minCreatedAt = createdAts.min() ?: error("Can't be empty value")
            val maxCreatedAt = createdAts.max() ?: error("Can't be empty value")

            logger.info("[OpenSea] Fetched ${openSeaOrders.size}, minId=$minId, maxId=$maxId, minCreatedAt=$minCreatedAt, maxCreatedAt=$maxCreatedAt, new OpenSea orders: ${openSeaOrders.joinToString { it.orderHash.toString() }}")

            coroutineScope {
                openSeaOrders
                    .chunked(properties.saveOpenSeaOrdersBatchSize)
                    .map { chunk ->
                        async {
                            chunk
                                .mapNotNull { openSeaOrderConverter.convert(it) }
                                .forEach { save(it) }
                        }
                    }.awaitAll()
            }
            logger.info("[OpenSea] All new OpenSea orders saved")
            maxCreatedAt.epochSecond
        } else {
            logger.info("[OpenSea] No new orders to fetch")
            delay(pollingPeriod)
            if (listedBefore > now) now else listedBefore
        }
        openSeaFetchStateRepository.save(state.withListedAfter(nextListedAfter + 1))
    }

    private suspend fun save(order: Order) {
        if (orderRepository.findById(order.hash) == null) {
            orderRepository.save(order)
        }
        if (properties.loadOpenSeaOrderVersion) {
            val version = orderVersionRepository.save(OrderVersion(
                hash = order.hash,
                maker = order.maker,
                taker = order.taker,
                make = order.make,
                take = order.take,
                makePriceUsd = order.makePriceUsd,
                takePriceUsd = order.takePriceUsd,
                makeUsd = order.makeUsd,
                takeUsd = order.takeUsd,
                platform = Platform.OPEN_SEA
            )).awaitFirst()

            orderVersionListener.onOrderVersion(version)
        }
        logger.info("Save new openSea order ${order.hash}")
    }

    private companion object {
        val MAX_LOAD_PERIOD: Duration = Duration.ofSeconds(30)
        val INIT_FETCH_STATE: OpenSeaFetchState = OpenSeaFetchState((Instant.now() - MAX_LOAD_PERIOD).epochSecond)
    }
}
