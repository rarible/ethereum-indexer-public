package com.rarible.protocol.order.core.service.pool

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.AmmOrderNftUpdateEventDto
import com.rarible.protocol.order.core.model.ItemId
import com.rarible.protocol.order.core.model.OnChainAmmOrder
import com.rarible.protocol.order.core.model.PoolDataUpdate
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.model.PoolNftDeposit
import com.rarible.protocol.order.core.model.PoolNftIn
import com.rarible.protocol.order.core.model.PoolNftOut
import com.rarible.protocol.order.core.model.PoolNftWithdraw
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import com.rarible.protocol.order.core.repository.order.OrderRepository
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class PoolEventListener(
    private val orderRepository: OrderRepository,
    private val orderPublisher: ProtocolOrderPublisher,
) {
    suspend fun onPoolEvent(event: LogEvent) {
        val reverted = event.status == LogEventStatus.REVERTED
        val poolHistory = event.data as PoolHistory
        val hash = poolHistory.hash
        val order = orderRepository.findById(hash)
            ?: throw IllegalStateException("Can't find order $hash")
        val collection = when {
            order.make.type.nft -> order.make.type.token
            order.take.type.nft -> order.take.type.token
            else -> return
        }
        val nftDelta = when (poolHistory) {
            is OnChainAmmOrder -> {
                NftDelta(inNft = poolHistory.tokenIds)
            }
            is PoolNftDeposit -> {
                if (poolHistory.collection == order.make.type.token) NftDelta(inNft = poolHistory.tokenIds) else NftDelta()
            }
            is PoolNftIn -> {
                NftDelta(inNft = poolHistory.tokenIds)
            }
            is PoolNftWithdraw -> {
                if (poolHistory.collection == order.make.type.token) NftDelta(outNft = poolHistory.tokenIds) else NftDelta()
            }
            is PoolNftOut -> {
                NftDelta(outNft = poolHistory.tokenIds)
            }
            is PoolDataUpdate -> return
        }
        if (nftDelta.isNotEmpty) {
            orderPublisher.publish(
                AmmOrderNftUpdateEventDto(
                    eventId = event.id.toHexString(),
                    orderId = hash.toString(),
                    inNft = nftDelta.getInNft(collection, reverted),
                    outNft = nftDelta.getOutNft(collection, reverted)
                )
            )
        }
    }

    private class NftDelta(
        private val inNft: List<EthUInt256> = emptyList(),
        private val outNft: List<EthUInt256> = emptyList(),
    ) {
        val isNotEmpty = inNft.isNotEmpty() || outNft.isNotEmpty()

        fun getInNft(collection: Address, reverted: Boolean): List<String> {
            return (if (reverted.not()) inNft else outNft).map { convert(collection, it) }
        }

        fun getOutNft(collection: Address, reverted: Boolean): List<String> {
            return (if (reverted.not()) outNft else inNft).map { convert(collection, it) }
        }

        private fun convert(collection: Address, tokenId: EthUInt256): String {
            return ItemId(collection, tokenId.value).toString()
        }
    }
}