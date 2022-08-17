package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.opensea.client.model.v2.SeaportOrders
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
import com.rarible.protocol.order.listener.misc.seaportError
import com.rarible.protocol.order.listener.misc.seaportInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.produce
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
    suspend fun load(cursor: String?) = coroutineScope {
        var lastSeaResult: SeaportOrders? = null
        for (result in produceNextSellOrders(cursor, properties.channelCapacity)) {
            lastSeaResult = result
            val orders = result.orders
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
            @Suppress("ConvertCallChainIntoSequence")
            orders
                .chunked(properties.saveBatchSize)
                .map { chunk ->
                    chunk.map {
                        async {
                            val order = openSeaOrderConverter.convert(it)
                            if (
                                order != null &&
                                properties.saveEnabled &&
                                openSeaOrderValidator.validate(order) &&
                                orderRepository.findById(order.hash) == null
                            ) {
                                orderUpdateService.save(order)
                                seaportSaveCounter.increment()
                                orderUpdateService.updateMakeStock(order.hash)
                                logger.seaportInfo("Saved new order ${order.hash}")
                            }
                        }
                    }.awaitAll()
                }
                .flatten()
                .lastOrNull()
        }
        lastSeaResult ?: throw IllegalStateException("Unexpected null result for cursor $cursor")
    }

    @Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
    private fun CoroutineScope.produceNextSellOrders(cursor: String?, capacity: Int) = produce(capacity = capacity) {
        var previous = cursor
        do {
            try {
                val result = openSeaOrderService.getNextSellOrders(previous)
                previous = result.previous
                send(result)
            } catch (ex: Throwable) {
                logger.seaportError("Can't get next orders with cursor $cursor", ex)
                throw ex
            }
        } while (previous != null)
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(SeaportOrderLoadHandler::class.java)
    }
}