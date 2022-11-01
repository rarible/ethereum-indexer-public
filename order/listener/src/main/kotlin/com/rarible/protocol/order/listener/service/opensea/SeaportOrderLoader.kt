package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.opensea.client.model.v2.SeaportOrders
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
import com.rarible.protocol.order.listener.misc.seaportError
import com.rarible.protocol.order.listener.misc.seaportInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
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
    suspend fun load(
        cursor: String?,
        loadAhead: Boolean,
        fetchCursor: (SeaportOrders) -> String? = { it.previous }
    ) = coroutineScope {
        var lastSeaResult: SeaportOrders? = null
        val handlesAsync = mutableListOf<Deferred<Unit?>>()
        for (result in produceNextSellOrders(cursor, fetchCursor, loadAhead, properties.maxLoadResults)) {
            lastSeaResult = result

            val handleAsync = async {
                val orders = result.orders
                val createdAts = orders.map { it.createdAt }
                val minCreatedAt = createdAts.minOrNull()
                val maxCreatedAt = createdAts.maxOrNull()

                logger.seaportInfo(
                    buildString {
                        append("Fetched ${orders.size}, ")
                        append("minCreatedAt=$minCreatedAt, ")
                        append("maxCreatedAt=$maxCreatedAt, ")
                        append("cursor=${result.previous}, ")
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
                                    val saved = orderUpdateService.save(order).run {
                                        orderUpdateService.updateMakeStock(this).first
                                    }
                                    seaportSaveCounter.increment()
                                    logger.seaportInfo("Saved new order ${saved.id}: ${saved.status}")
                                }
                            }
                        }.awaitAll()
                    }
                    .flatten()
                    .lastOrNull()
            }
            handlesAsync.add(handleAsync)
        }
        handlesAsync.awaitAll()
        lastSeaResult ?: throw IllegalStateException("Unexpected null result for cursor $cursor")
    }

    @Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
    private fun CoroutineScope.produceNextSellOrders(
        cursor: String?,
        fetchCursor: (SeaportOrders) -> String?,
        loadAhead: Boolean,
        maxLoadResults: Int
    ) = produce(capacity = maxLoadResults) {
        var results = 0
        var previous = cursor
        do {
            try {
                val result = openSeaOrderService.getNextSellOrders(previous, loadAhead)
                previous = fetchCursor(result)
                results += 1
                send(result)
            } catch (ex: Throwable) {
                logger.seaportError("Can't get next orders with cursor $cursor", ex)
                throw ex
            }
        } while (previous != null && results < maxLoadResults)
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(SeaportOrderLoadHandler::class.java)
    }
}
