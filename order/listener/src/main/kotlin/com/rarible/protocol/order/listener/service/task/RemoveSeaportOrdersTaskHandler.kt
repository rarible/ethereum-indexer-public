package com.rarible.protocol.order.listener.service.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.continuation.page.PageSize
import com.rarible.protocol.order.core.misc.orderTaskEventMarks
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderSeaportDataV1
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.order.OrderFilterSell
import com.rarible.protocol.order.core.model.order.OrderFilterSort
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import kotlinx.coroutines.flow.flow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RemoveSeaportOrdersTaskHandler(
    private val orderRepository: OrderRepository,
    private val orderVersionRepository: OrderVersionRepository,
    private val properties: OrderIndexerProperties,
    private val orderStateRepository: OrderStateRepository,
    private val orderUpdateService: OrderUpdateService,
) : TaskHandler<String> {

    override val type: String
        get() = REMOVE_SEAPORT_ORDERS

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun runLongTask(from: String?, param: String) = flow<String> {
        val stopAfter = Instant.ofEpochSecond(param.toLong())
        var continuation: String? = from
        do {
            val filter = OrderFilterSell(
                platforms = listOf(PlatformDto.OPEN_SEA),
                status = listOf(OrderStatusDto.ACTIVE, OrderStatusDto.INACTIVE, OrderStatusDto.CANCELLED),
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
        } while (continuation != null && last != null && last.lastUpdateAt > stopAfter)
    }

    private suspend fun handleOrders(orders: List<Order>) {
        orders
            .onEach {
                logger.info("Get order ${it.hash}, ${it.status}")
            }
            .filter {
                it.status == OrderStatus.ACTIVE || it.status == OrderStatus.INACTIVE || it.status == OrderStatus.NOT_STARTED
            }
            .filter {
                (it.data as? OrderSeaportDataV1)?.protocol == properties.exchangeContractAddresses.seaportV1_4
            }.forEach {
                val data = it.data as OrderSeaportDataV1

                if (properties.featureFlags.removeOpenSeaOrdersInTask) {
                    logger.info("Remove Seaport order ${it.hash} (protocol=${data.protocol}, lastUpdate=${it.lastUpdateAt})")
                    orderRepository.remove(it.hash)
                    orderVersionRepository.deleteByHash(it.hash)
                    orderStateRepository.remove(it.hash)
                } else {
                    val eventTimeMarks = orderTaskEventMarks()
                    val state = OrderState(id = it.hash, canceled = true)
                    orderStateRepository.save(state)
                    orderUpdateService.update(it.hash, eventTimeMarks)
                    val updatedOrder = orderRepository.findById(it.hash)
                    logger.info("Update Seaport order ${updatedOrder?.hash} (protocol=${data.protocol}, status=${updatedOrder?.status})")
                }
            }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RemoveSeaportOrdersTaskHandler::class.java)
        const val REMOVE_SEAPORT_ORDERS = "REMOVE_SEAPORT_ORDERS"
    }
}
