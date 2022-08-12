package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.opensea.client.model.v2.SeaportOrders
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SeaportOrderLoader(
    private val openSeaOrderService: OpenSeaOrderService,
    private val openSeaOrderConverter: OpenSeaOrderConverter,
    private val openSeaOrderValidator: OpenSeaOrderValidator,
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val properties: SeaportLoadProperties,
    private val seaportSaveCounter: RegisteredCounter
) {
    suspend fun load(cursor: String?): SeaportOrders {
        val result = safeGetNextSellOrders(cursor)
        val orders = result.orders
        if (orders.isNotEmpty()) {
            val createdAts = orders.map { it.createdAt }
            val minCreatedAt = createdAts.minOrNull()
            val maxCreatedAt = createdAts.maxOrNull()

            logger.seaportInfo(
                buildString {
                    append("Fetched ${orders.size}, ")
                    append("minCreatedAt=$minCreatedAt, ")
                    append("maxCreatedAt=$maxCreatedAt, ")
                    append("cursor=$cursor, ")
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
                                    orderUpdateService.updateMakeStock(it.hash)
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
        }
        return result
    }

    private suspend fun safeGetNextSellOrders(cursor: String?): SeaportOrders {
        return try {
            openSeaOrderService.getNextSellOrders(cursor)
        } catch (ex: Throwable) {
            logger.seaportError("Can't get next orders with cursor $cursor", ex)
            throw ex
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(SeaportOrderLoadHandler::class.java)
    }
}