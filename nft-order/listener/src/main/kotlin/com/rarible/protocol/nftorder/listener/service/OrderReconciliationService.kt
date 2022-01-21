package com.rarible.protocol.nftorder.listener.service

import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.nftorder.listener.configuration.NftOrderJobProperties
import com.rarible.protocol.order.api.client.OrderControllerApi
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OrderReconciliationService(
    private val orderControllerApi: OrderControllerApi,
    private val orderEventService: OrderEventService,
    nftOrderJobProperties: NftOrderJobProperties
) {

    private val logger = LoggerFactory.getLogger(OrderReconciliationService::class.java)
    val orderReconciliationConfig = nftOrderJobProperties.reconciliation.order

    suspend fun reconcileOrders(lastUpdateContinuation: String?): String? {
        logger.info("Fetching Orders from [{}]", lastUpdateContinuation)
        val page = orderControllerApi.getOrdersAll(
            null,
            null,
            lastUpdateContinuation,
            orderReconciliationConfig.batchSize
        ).awaitFirst()

        if (page.orders.isEmpty()) {
            logger.info("There is no more Orders for continuation $lastUpdateContinuation, aborting reconciliation")
            return null
        }

        val nextContinuation = page.continuation
        page.orders.forEach {
            try {
                orderEventService.updateOrder(it)
            } catch (e: Exception) {
                logger.error("Unable to reconcile order: ${it}", e)
            }
        }
        logger.info("${page.orders.size} Orders updated, next continuation is [{}]", nextContinuation)

        return nextContinuation
    }
}
