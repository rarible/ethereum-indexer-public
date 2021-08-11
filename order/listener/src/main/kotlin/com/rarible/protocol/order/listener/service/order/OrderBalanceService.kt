package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.provider.ProtocolCommissionProvider
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.PriceUpdateService
import kotlinx.coroutines.flow.collect
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.time.Instant

@Component
class OrderBalanceService(
    private val orderRepository: OrderRepository,
    private val priceUpdateService: PriceUpdateService,
    private val protocolCommissionProvider: ProtocolCommissionProvider
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun handle(event: Erc20BalanceEventDto) {
        when (event) {
            is Erc20BalanceUpdateEventDto -> {
                val at = nowMillis()
                val maker = event.balance.owner
                val token = event.balance.contract
                val stock = EthUInt256.of(event.balance.balance)

                orderRepository
                    .findByTargetBalanceAndNotCanceled(maker, token)
                    .collect { optimisticLockUpdate(it, stock, at) }
            }
        }
    }

    suspend fun handle(event: NftOwnershipEventDto) {
        when (event) {
            is NftOwnershipUpdateEventDto -> {
                val at = nowMillis()
                val maker = event.ownership.owner
                val token = Address.apply(event.ownership.contract)
                val tokenId = event.ownership.tokenId
                val stock = EthUInt256.of(event.ownership.value)

                orderRepository
                    .findByTargetNftAndNotCanceled(maker, token, EthUInt256(tokenId))
                    .collect { optimisticLockUpdate(it, stock, at) }
            }
            is NftOwnershipDeleteEventDto -> {

            }
        }
    }

    protected suspend fun optimisticLockUpdate(order: Order, stock: EthUInt256, at: Instant) {
        try {
            updateInternal(order, stock, at)
        } catch (_: OptimisticLockingFailureException) {
            optimisticLock {
                val currentOrder = orderRepository.findById(order.hash)
                if (currentOrder != null) {
                    updateInternal(currentOrder, stock, at)
                }
            }
        }
    }

    suspend fun updateInternal(currentOrder: Order, stock: EthUInt256, at: Instant) {
        currentOrder
            .withMakeBalance(stock, protocolCommissionProvider.get())
            .let {
                if (currentOrder.makeStock == EthUInt256.ZERO && it.makeStock != EthUInt256.ZERO)
                    priceUpdateService.updateOrderPrice(it, at)
                else it
            }
            .let { orderRepository.save(it, currentOrder) }
            .also { logger.info("Updated order: hash=${it.hash}, makeBalance=$stock, makeStock=${it.makeStock}") }
    }
}
