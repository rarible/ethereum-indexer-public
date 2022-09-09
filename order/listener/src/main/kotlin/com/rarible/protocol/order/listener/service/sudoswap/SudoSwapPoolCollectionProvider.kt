package com.rarible.protocol.order.listener.service.sudoswap

import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.LSSVMPairV1
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.repository.order.OrderRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.ReadOnlyMonoTransactionSender

@Component
class SudoSwapPoolCollectionProvider(
    private val sender: ReadOnlyMonoTransactionSender,
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val orderRepository: OrderRepository
) {
    suspend fun getPoolCollection(poolAddress: Address): Address {
        val hash = sudoSwapEventConverter.getPoolHash(poolAddress)
        val order = orderRepository.findById(hash)
        if (order != null) {
            when {
                order.make.type.nft -> return order.make.type.token
                else -> { }
            }
        }
        val contract = LSSVMPairV1(poolAddress, sender)
        return contract.nft().call().awaitSingle()
    }
}