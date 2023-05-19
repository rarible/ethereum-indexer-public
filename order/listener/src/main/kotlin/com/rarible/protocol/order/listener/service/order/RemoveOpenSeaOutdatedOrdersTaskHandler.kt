package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderBasicSeaportDataV1
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Deprecated("in favor of RemoveOutdatedOrdersTaskHandler")
@Component
class RemoveOpenSeaOutdatedOrdersTaskHandler(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
) : TaskHandler<String> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val type = "REMOVE_OPEN_SEA_OUTDATED_ORDERS"

    private val legacyOrderContracts = setOf(
        exchangeContractAddresses.seaportV1,
        exchangeContractAddresses.seaportV1_4
    )

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val status = OrderStatus.valueOf(param)
        logger.info("Start $type task with $status param")
        return orderRepository
            .findAll(Platform.OPEN_SEA, status, fromHash = null)
            .filter { isCandidateToBeCancelled(it) }
            .map { updateOrder(it) }
    }

    private fun isCandidateToBeCancelled(order: Order): Boolean {
        // All opensea legacy orders should be cancelled
        if (order.type == OrderType.OPEN_SEA_V1) {
            return true
        }
        if (order.type == OrderType.SEAPORT_V1) {
            // Seaport V1.1 is not supported anymore
            val protocol = (order.data as? OrderBasicSeaportDataV1)?.protocol
            return protocol != null && legacyOrderContracts.contains(protocol)
        }
        return false
    }

    private suspend fun updateOrder(order: Order): String {
        orderUpdateService.update(order.hash)
        return order.id.toString()
    }
}
