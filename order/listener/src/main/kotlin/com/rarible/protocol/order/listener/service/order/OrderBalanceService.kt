package com.rarible.protocol.order.listener.service.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class OrderBalanceService(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService
) {

    suspend fun handle(event: Erc20BalanceEventDto) {
        when (event) {
            is Erc20BalanceUpdateEventDto -> {
                val maker = event.balance.owner
                val token = event.balance.contract
                val stock = EthUInt256.of(event.balance.balance)

                orderRepository
                    .findByTargetBalanceAndNotCanceled(maker, token)
                    .collect { orderUpdateService.updateMakeStock(it.hash, knownMakeBalance = stock) }
            }
        }
    }

    suspend fun handle(event: NftOwnershipEventDto) {
        when (event) {
            is NftOwnershipUpdateEventDto -> {
                val maker = event.ownership.owner
                val token = Address.apply(event.ownership.contract)
                val tokenId = event.ownership.tokenId
                val stock = EthUInt256.of(event.ownership.value)

                orderRepository
                    .findByTargetNftAndNotCanceled(maker, token, EthUInt256(tokenId))
                    .collect { orderUpdateService.updateMakeStock(it.hash, knownMakeBalance = stock) }
            }
            is NftOwnershipDeleteEventDto -> Unit
        }
    }

}
