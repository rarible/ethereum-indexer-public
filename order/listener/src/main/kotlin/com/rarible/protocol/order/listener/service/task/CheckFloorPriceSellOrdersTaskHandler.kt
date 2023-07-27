package com.rarible.protocol.order.listener.service.task

import com.rarible.core.task.TaskHandler
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.SeaportFulfillmentSimpleResponseDto
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.service.floor.FloorSellOrderProvider
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.time.Instant
import java.util.Date

@Component
class CheckFloorPriceSellOrdersTaskHandler(
    private val exchangeHistoryRepository: ExchangeHistoryRepository,
    private val floorSellOrderProvider: FloorSellOrderProvider,
    private val properties: OrderListenerProperties,
    private val blockchain: Blockchain,
) : TaskHandler<String> {

    private val client = RestTemplate()

    override val type: String
        get() = CHECK_FLOOR_PRICE_SELL_ORDERS

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val now = Instant.now()
        return exchangeHistoryRepository.getTokenPurchaseAggregation(
            startDate = Date.from(now - Duration.ofDays(30)),
            endDate = Date.from(now),
            source = null
        ).asFlow().take(properties.floorPriceTopCollectionsCount).map {
            val collection = it.address
            val floorSells = floorSellOrderProvider.getCurrencyFloorSells(collection)
            logger.info("Found ${floorSells.size} floor sells for collection $collection")
            floorSells.filter { order -> order.platform == Platform.OPEN_SEA }.forEach { order ->
                logger.info("Floor sell for collection $collection: $it")
                checkOrderSignatureAvailable(order.hash)
            }
            collection.prefixed()
        }
    }

    private fun checkOrderSignatureAvailable(hash: Word) {
        try {
            val url = String.format("http://%s-order-api:8080/v0.1/signature/seaport/simple/%s", blockchain, hash.prefixed())
            val result = client.getForObject(url, SeaportFulfillmentSimpleResponseDto::class.java)
            logger.info("Get order signature for $hash: $result")
        } catch (ex: Exception) {
            logger.error("Can't get order signature for $hash", ex)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CheckFloorPriceSellOrdersTaskHandler::class.java)
        const val CHECK_FLOOR_PRICE_SELL_ORDERS = "CHECK_FLOOR_PRICE_SELL_ORDERS"
    }
}
