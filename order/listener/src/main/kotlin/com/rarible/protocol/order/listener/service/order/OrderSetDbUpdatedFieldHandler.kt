package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.repository.order.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OrderSetDbUpdatedFieldHandler (
    private val orderRepository: OrderRepository,
) : TaskHandler<String> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val type: String
        get() = ORDER_SET_DB_UPDATE_FIELD

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return orderRepository.findWithoutDbUpdatedField().mapNotNull {
            updateOrder(it)
        }
    }

    private suspend fun updateOrder(order: Order): String?  {
        if(order.dbUpdatedAt != null ) {
            logger.info("[$ORDER_SET_DB_UPDATE_FIELD] Shouldn't update this order $order!")
            return null
        }

        val updatedOrder = order.copy(dbUpdatedAt = order.lastUpdateAt)
        orderRepository.saveWithoutDbUpdated(updatedOrder)
        logger.info("[$ORDER_SET_DB_UPDATE_FIELD] Order $updatedOrder has been updated!")

        return updatedOrder.toString()
    }

    companion object {
        const val ORDER_SET_DB_UPDATE_FIELD = "ORDER_SET_DB_UPDATE_FIELD"
    }
}