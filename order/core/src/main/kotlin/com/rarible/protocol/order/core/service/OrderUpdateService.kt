package com.rarible.protocol.order.core.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.optimisticLock
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.event.OrderListener
import com.rarible.protocol.order.core.event.OrderVersionListener
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.provider.ProtocolCommissionProvider
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.balance.AssetMakeBalanceProvider
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Service responsible for inserting or updating order state (see [save]).
 */
@Component
@CaptureSpan(type = SpanType.APP)
class OrderUpdateService(
    private val orderRepository: OrderRepository,
    private val assetMakeBalanceProvider: AssetMakeBalanceProvider,
    private val orderVersionRepository: OrderVersionRepository,
    private val orderReduceService: OrderReduceService,
    private val protocolCommissionProvider: ProtocolCommissionProvider,
    private val priceUpdateService: PriceUpdateService,
    private val orderVersionListener: OrderVersionListener,
    private val orderListener: OrderListener
) {

    private val logger = LoggerFactory.getLogger(OrderUpdateService::class.java)

    /**
     * Inserts a new order or updates an existing order with data from the [orderVersion].
     * Orders are identified by [OrderVersion.hash].
     *
     * [orderVersion] **must be** a valid order update having the same values for significant fields
     * (`data`, `start`, `end` etc.).
     * **Validation is not part of this function**. So make sure the source of order version updates is trustworthy.
     * On API level validation is performed in `OrderValidator.validate(existing: Order, update: OrderVersion)`.
     */
    suspend fun save(orderVersion: OrderVersion): Order {
        orderVersionRepository.save(orderVersion).awaitFirst()
        val order = optimisticLock { orderReduceService.updateOrder(orderVersion.hash) }
        checkNotNull(order) { "Order ${orderVersion.hash} has not been updated" }

        orderListener.onOrder(order)
        orderVersionListener.onOrderVersion(orderVersion)
        return order
    }

    suspend fun update(hash: Word) {
        val order = orderRepository.findById(hash)
        val updatedOrder = orderReduceService.updateOrder(hash)

        if (updatedOrder != null && order?.lastEventId != updatedOrder.lastEventId) {
            orderListener.onOrder(updatedOrder)
        }
    }

    suspend fun updateMakeStock(
        hash: Word,
        makeBalanceState: MakeBalanceState? = null
    ): Order? =
        updateMakeStockFull(hash, makeBalanceState).first

    /**
     * Updates the order's make stock and prices without calling the OrderReduceService.
     */
    suspend fun updateMakeStockFull(
        hash: Word,
        makeBalanceState: MakeBalanceState? = null
    ): Pair<Order?, Boolean> {
        val order = orderRepository.findById(hash) ?: return null to false

        val makeBalance = makeBalanceState ?: assetMakeBalanceProvider.getMakeBalance(order)
        val knownMakeBalance = makeBalance.value

        // We don't want to change lastUpdateAt for CANCELLED or FINISHED orders
        val lastUpdatedAt = if (isFinished(order)) {
            order.lastUpdateAt
        } else {
            getLatestDate(order.lastUpdateAt, makeBalance.lastUpdatedAt)!!
        }

        val protocolCommission = protocolCommissionProvider.get()
        val withNewMakeStock = order.withMakeBalance(knownMakeBalance, protocolCommission)

        val updated = if (order.makeStock == EthUInt256.ZERO && withNewMakeStock.makeStock != EthUInt256.ZERO) {
            priceUpdateService.withUpdatedAllPrices(withNewMakeStock)
        } else {
            withNewMakeStock
        }.copy(lastUpdateAt = lastUpdatedAt)

        // We need to allow updates even if only lastUpdatedAt has been changed
        // otherwise we won't be able to update some of existing orders by background reduce job
        return if (order.makeStock != updated.makeStock || order.lastUpdateAt != updated.lastUpdateAt) {
            val savedOrder = orderRepository.save(updated)
            orderListener.onOrder(savedOrder)
            logger.info("Make stock of order updated ${savedOrder.hash}: makeStock=${savedOrder.makeStock}, old makeStock=${order.makeStock}, makeBalance=$makeBalance, knownMakeBalance=$knownMakeBalance, cancelled=${savedOrder.cancelled}")
            savedOrder to true
        } else {
            logger.info("Make stock of order did not change ${updated.hash}: makeStock=${updated.makeStock}, makeBalance=$makeBalance, knownMakeBalance=$knownMakeBalance, cancelled=${updated.cancelled}")
            order to false
        }
    }

    private fun isFinished(order: Order): Boolean {
        return order.status == OrderStatus.CANCELLED
            || order.status == OrderStatus.FILLED
    }

    private fun getLatestDate(date1: Instant?, date2: Instant?): Instant? {
        if (date1 == null) return date2
        if (date2 == null) return date1
        return maxOf(date1, date2)
    }
}
