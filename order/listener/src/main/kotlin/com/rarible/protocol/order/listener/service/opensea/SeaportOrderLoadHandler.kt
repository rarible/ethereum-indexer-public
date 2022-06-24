package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SeaportOrderLoadHandler(
    private val openSeaOrderService: OpenSeaOrderService,
    private val openSeaOrderConverter: OpenSeaOrderConverter,
    private val openSeaOrderValidator: OpenSeaOrderValidator,
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService
) : JobHandler {

    override suspend fun handle() {
        val orders = openSeaOrderService.getNextSellOrders(null).orders
        val createdAts = orders.map { it.createdAt }
        val minCreatedAt = createdAts.minOrNull() ?: error("Can't be empty value")
        val maxCreatedAt = createdAts.maxOrNull() ?: error("Can't be empty value")

        logInfo("Fetched ${orders.size}, minCreatedAt=$minCreatedAt, maxCreatedAt=$maxCreatedAt, new Seaport orders: ${orders.joinToString { it.orderHash.toString() }}")

        if (orders.isNotEmpty()) {
            orders
                .mapNotNull {
                    openSeaOrderConverter.convert(it)
                }.filter {
                    openSeaOrderValidator.validate(it)
                }.forEach {
                    if (orderRepository.findById(it.hash) == null) {
                        orderUpdateService.save(it)
                        logInfo("Saved new Seaport order ${it.hash}")
                    }
                }
        }
    }

    private fun logInfo(message: String) {
        logger.info("[Seaport] {}", message)
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(SeaportOrderLoadHandler::class.java)
    }
}