package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.daemon.job.JobHandler
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.time.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SeaportOrderLoadHandler(
    private val openSeaOrderService: OpenSeaOrderService,
    private val openSeaOrderConverter: OpenSeaOrderConverter,
    private val openSeaOrderValidator: OpenSeaOrderValidator,
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val properties: SeaportLoadProperties,
    private val seaportSaveCounter: RegisteredCounter
) : JobHandler {

    override suspend fun handle() {
        val orders = openSeaOrderService.getNextSellOrders().orders
        if (orders.isNotEmpty()) {
            val createdAts = orders.map { it.createdAt }
            val minCreatedAt = createdAts.minOrNull()
            val maxCreatedAt = createdAts.maxOrNull()

            logger.seaportInfo(
                buildString {
                    append("Fetched ${orders.size}, ")
                    append("minCreatedAt=$minCreatedAt, ")
                    append("maxCreatedAt=$maxCreatedAt, ")
                    append("new orders: ${orders.joinToString { it.orderHash.toString() }}")
                }
            )
            coroutineScope {
                @Suppress("ConvertCallChainIntoSequence")
                orders
                    .mapNotNull {
                        openSeaOrderConverter.convert(it)
                    }.filter {
                        openSeaOrderValidator.validate(it)
                    }
                    .chunked(properties.saveBatchSize)
                    .map { chunk ->
                        chunk.map {
                            async {
                                if (properties.saveEnabled && orderRepository.findById(it.hash) == null) {
                                    orderUpdateService.save(it)
                                    seaportSaveCounter.increment()
                                    logger.seaportInfo("Saved new order ${it.hash}")
                                }
                            }
                        }.awaitAll()
                    }
                    .flatten()
                    .lastOrNull()
            }
        } else {
            logger.seaportInfo("No new orders was fetched")
            delay(properties.pollingPeriod)
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(SeaportOrderLoadHandler::class.java)
    }
}