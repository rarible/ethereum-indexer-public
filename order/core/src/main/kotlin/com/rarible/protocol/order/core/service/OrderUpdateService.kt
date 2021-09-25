package com.rarible.protocol.order.core.service

import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.event.OrderListener
import com.rarible.protocol.order.core.event.OrderVersionListener
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.provider.ProtocolCommissionProvider
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.balance.AssetMakeBalanceProvider
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Service responsible for inserting or updating order state (see [save]).
 */
@Component
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
     * (`data`, `start`, `end`, etc).
     * **Validation is not part of this function**. So make sure the source of order version updates is trustworthy.
     * On API level validation is performed in `OrderValidator.validate(existing: Order, update: OrderVersion)`.
     */
    suspend fun save(orderVersion: OrderVersion): Order {
        orderVersionRepository.save(orderVersion).awaitFirst()
        val order = optimisticLock { orderReduceService.updateOrder(orderVersion.hash) }

        orderListener.onOrder(order)
        orderVersionListener.onOrderVersion(orderVersion)
        return order
    }

    suspend fun update(hash: Word): Order {
        val order = orderRepository.findById(hash)
        val update = orderReduceService.updateOrder(hash)

        if (order?.lastEventId != update.lastEventId) {
            orderListener.onOrder(update)
        }
        return update
    }

    /**
     * Updates the order's make stock and prices without calling the OrderReduceService.
     */
    suspend fun updateMakeStock(hash: Word, knownMakeBalance: EthUInt256? = null): Order? {
        val order = orderRepository.findById(hash) ?: return null
        val makeBalance = knownMakeBalance ?: assetMakeBalanceProvider.getMakeBalance(order) ?: EthUInt256.ZERO
        val protocolCommission = protocolCommissionProvider.get()
        val withNewBalance = order.withMakeBalance(makeBalance, protocolCommission)
        val updated = if (order.makeStock == EthUInt256.ZERO && withNewBalance.makeStock != EthUInt256.ZERO) {
            priceUpdateService.updateOrderPrice(withNewBalance, nowMillis())
        } else {
            withNewBalance
        }
        logger.info("Updated order ${updated.hash}, makeStock=${updated.makeStock}, makeBalance=$makeBalance")
        return orderRepository.save(updated)
    }
}
