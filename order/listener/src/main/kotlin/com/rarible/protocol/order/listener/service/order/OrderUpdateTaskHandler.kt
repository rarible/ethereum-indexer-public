package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.optimisticLock
import com.rarible.core.task.TaskHandler
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.provider.ProtocolCommissionProvider
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.asset.AssetBalanceProvider
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.time.delay
import org.jboss.logging.Logger
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import java.lang.RuntimeException
import java.time.Duration
import java.util.*

@Component
class OrderUpdateTaskHandler(
    private val orderRepository: OrderRepository,
    private val assertBalanceProvider: AssetBalanceProvider,
    private val protocolCommissionProvider: ProtocolCommissionProvider,
    private val properties: OrderListenerProperties
) : TaskHandler<Long> {
    private val logger = Logger.getLogger(javaClass)

    override val type: String
        get() = ORDER_UPDATE

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun runLongTask(from: Long?, param: String): Flow<Long> {
        return orderRepository.findAllBeforeLastUpdateAt(from?.let { Date(it) })
            .map { order ->
                handleOrder(order)
                order.lastUpdateAt.toEpochMilli()
            }
    }

    private suspend fun handleOrder(order: Order) {
        val makeBalance = try {
            assertBalanceProvider.getAssetStock(order.maker, order.make.type) ?: EthUInt256.ZERO
        } catch (ex: Exception) {
            throw RuntimeException("Can't get order ${order.hash} make balance", ex)
        }
        optimisticLockUpdate(order, makeBalance)
        delay(Duration.ofMillis(properties.publishTaskDelayMs))
    }

    protected suspend fun optimisticLockUpdate(order: Order, makeBalance: EthUInt256) {
        try {
            updateInternal(order, makeBalance)
        } catch (_: OptimisticLockingFailureException) {
            optimisticLock {
                val currentOrder = orderRepository.findById(order.hash)
                if (currentOrder != null) {
                    updateInternal(currentOrder, makeBalance)
                }
            }
        }
    }

    suspend fun updateInternal(currentOrder: Order, makeBalance: EthUInt256) {
        try {
            currentOrder
                .withMakeBalance(makeBalance, protocolCommissionProvider.get())
                .let { orderRepository.save(it, currentOrder) }
                .also { logger.info("Order ${currentOrder.hash} was updates by task '$ORDER_UPDATE'") }
        } catch (ex: Exception) {
            throw RuntimeException("Can't update order ${currentOrder.hash} with make balance $makeBalance", ex)
        }
    }

    companion object {
        const val ORDER_UPDATE = "ORDER_UPDATE"
    }
}