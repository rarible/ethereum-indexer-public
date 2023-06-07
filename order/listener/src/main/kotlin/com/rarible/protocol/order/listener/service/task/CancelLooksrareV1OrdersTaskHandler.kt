package com.rarible.protocol.order.listener.service.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.dto.offchainEventMark
import com.rarible.protocol.order.core.continuation.page.PageSize
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.order.OrderFilterSell
import com.rarible.protocol.order.core.model.order.OrderFilterSort
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CancelLooksrareV1OrdersTaskHandler(
    private val orderRepository: OrderRepository,
    private val orderStateRepository: OrderStateRepository,
    private val orderVersionRepository: OrderVersionRepository,
    private val orderUpdateService: OrderUpdateService,
) : TaskHandler<String> {

    override val type: String
        get() = "CANCEL_LOOKSRARE_ORDERS"

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun runLongTask(from: String?, param: String) = flow<String> {
        var continuation: String? = from
        do {
            val filter = OrderFilterSell(
                platforms = listOf(PlatformDto.LOOKSRARE),
                status = listOf(OrderStatusDto.ACTIVE, OrderStatusDto.INACTIVE),
                sort = OrderFilterSort.LAST_UPDATE_DESC,
            ).toQuery(continuation, PageSize.ORDER.max)

            val orders = orderRepository.search(filter)
            handleOrders(orders)
            val last = orders.lastOrNull()
            continuation = last?.let {
                val next = Continuation.LastDate(it.lastUpdateAt, it.hash).toString()
                emit(next)
                next
            }
        } while (continuation != null)
    }

    private suspend fun handleOrders(orders: List<Order>) {
        orders
            .filter { it.type == OrderType.LOOKSRARE }
            .filter { it.status in CANCELABLE_ORDER_STATUSES }
            .forEach {
                orderStateRepository.save(OrderState(id = it.hash, canceled = true))
                cleanOrderVersionSignature(it.hash)
                val eventTimeMarks = offchainEventMark("indexer-out_order")
                orderUpdateService.update(it.hash, eventTimeMarks)
                val updatedOrder = orderRepository.findById(it.hash)
                logger.info("Canceled LookRare order ${updatedOrder?.hash}, status=${updatedOrder?.status}), signature=${updatedOrder?.signature}")
            }
    }

    private suspend fun cleanOrderVersionSignature(hash: Word) {
        val versions = orderVersionRepository.findAllByHash(hash).toList()
        versions.forEach {
            val updatedVersion = it.copy(signature = null)
            orderVersionRepository.save(updatedVersion).awaitFirst()
        }
    }

    companion object {
        private val CANCELABLE_ORDER_STATUSES = listOf(OrderStatus.ACTIVE, OrderStatus.INACTIVE, OrderStatus.NOT_STARTED)
        private val logger: Logger = LoggerFactory.getLogger(CancelLooksrareV1OrdersTaskHandler::class.java)
    }
}
