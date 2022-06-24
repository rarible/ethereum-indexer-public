package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.daemon.job.JobHandler
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
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
        val createdAts = orders.map { it.createdAt }
        val minCreatedAt = createdAts.minOrNull() ?: error("Can't be empty value")
        val maxCreatedAt = createdAts.maxOrNull() ?: error("Can't be empty value")

        logger.seaportInfo(
            buildString {
                append("Fetched ${orders.size}, ")
                append("minCreatedAt=$minCreatedAt, ")
                append("maxCreatedAt=$maxCreatedAt, ")
                append("new Seaport orders: ${orders.joinToString { it.orderHash.toString() }}")
            }
        )
        if (orders.isNotEmpty()) {
            orders
                .mapNotNull {
                    openSeaOrderConverter.convert(it)
                }.filter {
                    openSeaOrderValidator.validate(it)
                }.forEach {
                    if (orderRepository.findById(it.hash) == null) {
                        orderUpdateService.save(it)
                        seaportSaveCounter.increment()
                        logger.seaportInfo("Saved new Seaport order ${it.hash}")
                    }
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