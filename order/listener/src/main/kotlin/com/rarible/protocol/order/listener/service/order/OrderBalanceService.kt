package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.EventTimeMarks
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.dto.toModel
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.addIn
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import kotlinx.coroutines.flow.filter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant

@Component
class OrderBalanceService(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val orderIndexerProperties: OrderIndexerProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun handle(event: Erc20BalanceEventDto) {
        val eventTimeMarks = event.eventTimeMarks?.toModel()?.addIn() ?: run {
            logger.warn("EventTimeMarks not found in Erc20BalanceEventDto")
            orderOffchainEventMarks()
        }
        when (event) {
            is Erc20BalanceUpdateEventDto -> {
                val maker = event.balance.owner
                val token = event.balance.contract
                val stock = EthUInt256.of(event.balance.balance)

                orderRepository
                    .findByTargetBalanceAndNotCanceled(maker, token)
                    .filter { order -> order.isNoLegacyOpenSea() }
                    .collect {
                        orderUpdateService.updateMakeStock(
                            hash = it.hash,
                            makeBalanceState = MakeBalanceState(stock, event.lastUpdatedAt),
                            eventTimeMarks = eventTimeMarks
                        )
                    }
            }
        }
    }

    suspend fun handle(event: NftOwnershipEventDto) {
        val eventTimeMarks = event.eventTimeMarks?.toModel()?.addIn() ?: run {
            logger.warn("EventTimeMarks not found in NftOwnershipEventDto")
            orderOffchainEventMarks()
        }
        when (event) {
            is NftOwnershipUpdateEventDto -> {
                onOwnershipUpdated(
                    event.ownership,
                    eventTimeMarks
                )
            }

            is NftOwnershipDeleteEventDto -> {
                if (event.deletedOwnership != null) {
                    onOwnershipUpdated(
                        event.deletedOwnership!!,
                        eventTimeMarks
                    )
                } else {
                    // TODO this branch should be removed later
                    val legacyOwnership = event.ownership!!
                    onOwnershipUpdated(
                        legacyOwnership.token,
                        legacyOwnership.tokenId,
                        legacyOwnership.owner,
                        BigInteger.ZERO,
                        null, // Should not be updated for legacy events
                        eventTimeMarks
                    )
                }
            }
        }
    }

    private suspend fun onOwnershipUpdated(ownership: NftOwnershipDto, eventTimeMarks: EventTimeMarks) {
        val token = Address.apply(ownership.contract)
        val tokenId = ownership.tokenId
        val maker = ownership.owner
        val stock = ownership.value

        onOwnershipUpdated(token, tokenId, maker, stock, ownership.date, eventTimeMarks)
    }

    private suspend fun onOwnershipUpdated(
        token: Address,
        tokenId: BigInteger,
        maker: Address,
        stock: BigInteger,
        updateDate: Instant?,
        eventTimeMarks: EventTimeMarks
    ) {
        orderRepository
            .findByTargetNftAndNotCanceled(maker, token, EthUInt256(tokenId))
            .filter { order -> order.isNoLegacyOpenSea() }
            .collect {
                orderUpdateService.updateMakeStock(
                    hash = it.hash,
                    makeBalanceState = MakeBalanceState(EthUInt256.of(stock), updateDate),
                    eventTimeMarks = eventTimeMarks
                )
            }
    }

    private fun Order.isNoLegacyOpenSea(): Boolean {
        return this.isLegacyOpenSea(orderIndexerProperties.exchangeContractAddresses.openSeaV1).not()
    }
}
