package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.nowMillis
import com.rarible.core.task.TaskHandler
import com.rarible.ethereum.domain.Blockchain
import com.rarible.opensea.client.OpenSeaClient
import com.rarible.opensea.client.autoconfigure.OpenSeaClientProperties
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.service.opensea.ExternalUserAgentProvider
import com.rarible.protocol.order.listener.service.opensea.MeasurableOpenSeaOrderService
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderConverter
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderServiceImpl
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class OpenSeaOrderLoadTaskHandler(
    private val openSeaOrderConverter: OpenSeaOrderConverter,
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val properties: OrderListenerProperties,
    externalUserAgentProvider: ExternalUserAgentProvider,
    openSeaClientProperties: OpenSeaClientProperties,
    micrometer: MeterRegistry,
    blockchain: Blockchain
) : TaskHandler<Long> {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val openSeaOrderService = MeasurableOpenSeaOrderService(
        OpenSeaOrderServiceImpl(
            OpenSeaClient(
                endpoint = properties.openSeaEndpoint ?: error("OpenSea endpoint must be defined"),
                apiKey = null,
                proxy = openSeaClientProperties.proxy,
                userAgentProvider = externalUserAgentProvider
            ),
            properties
        ),
        micrometer,
        blockchain
    )

    override val type: String
        get() = OPEN_SEA_ORDER_LOAD

    override suspend fun isAbleToRun(param: String): Boolean = properties.loadOldOpenSeaOrders

    override fun runLongTask(from: Long?, param: String): Flow<Long> {
        var listedAfter = from ?: properties.loadOpenSeaInitLoad
        return flow {
            while (true) {
                val now = nowMillis().epochSecond
                val listedBefore = listedAfter + MAX_LOAD_PERIOD.seconds

                logger.info("[OldOpenSea] Starting fetching OpenSea orders, listedAfter=$listedAfter, listedBefore=$listedBefore")
                val openSeaOrders = openSeaOrderService.getNextOrdersBatch(listedAfter = listedAfter, listedBefore = listedBefore)

                if (openSeaOrders.isNotEmpty()) {
                    val ids = openSeaOrders.map { it.id }
                    val minId = ids.min() ?: error("Can't be empty value")
                    val maxId = ids.max() ?: error("Can't be empty value")

                    val createdAts = openSeaOrders.map { it.createdAt }
                    val minCreatedAt = createdAts.min() ?: error("Can't be empty value")
                    val maxCreatedAt = createdAts.max() ?: error("Can't be empty value")

                    logger.info("[OldOpenSea] Fetched ${openSeaOrders.size}, minId=$minId, maxId=$maxId, minCreatedAt=$minCreatedAt, maxCreatedAt=$maxCreatedAt, new OpenSea orders: ${openSeaOrders.joinToString { it.orderHash.toString() }}")

                    coroutineScope {
                        openSeaOrders
                            .chunked(properties.saveOpenSeaOrdersBatchSize)
                            .map { chunk ->
                                async {
                                    chunk
                                        .mapNotNull { openSeaOrderConverter.convert(it) }
                                        .forEach { saveOrder(it) }
                                }
                            }.awaitAll()
                    }
                    logger.info("[OldOpenSea] All new OpenSea orders saved")
                } else {
                    logger.info("[OldOpenSea] No new orders to fetch")
                    delay(properties.pollingOpenSeaPeriod)
                }
                if (listedBefore <= now) {
                    listedAfter = listedBefore
                    emit(listedAfter)
                } else {
                    break
                }
            }
        }
    }

    private suspend fun saveOrder(orderVersion: OrderVersion) {
        if (orderRepository.findById(orderVersion.hash) == null) {
            orderUpdateService.save(orderVersion)
            logger.info("Saved new OpenSea order ${orderVersion.hash}")
        }
    }

    companion object {
        val MAX_LOAD_PERIOD: Duration = Duration.ofSeconds(30)
        const val OPEN_SEA_ORDER_LOAD = "OPEN_SEA_ORDER_LOAD"
    }
}

