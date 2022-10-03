package com.rarible.protocol.order.core.service.pool

import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.LSSVMPairV1
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderAmmData
import com.rarible.protocol.order.core.model.OrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.PoolInfo
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.sudoswap.SudoSwapProtocolFeeProvider
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.ReadOnlyMonoTransactionSender

@Component
class PoolInfoProvider(
    private val sender: ReadOnlyMonoTransactionSender,
    private val orderRepository: OrderRepository,
    private val sudoSwapProtocolFeeProvider: SudoSwapProtocolFeeProvider,
) {
    suspend fun getPollInfo(order: Order): PoolInfo? {
        return getInfoFromOrder(order)
    }

    suspend fun getPollInfo(hash: Word, poolAddress: Address): PoolInfo {
        val order = orderRepository.findById(hash)
        return order?.let { getInfoFromOrder(order) } ?: getInfoFromChain(poolAddress)
    }

    private suspend fun getInfoFromOrder(order: Order): PoolInfo? {
        return if (order.type == OrderType.AMM) {
            when (val data = order.data as OrderAmmData) {
                is OrderSudoSwapAmmDataV1 -> {
                    PoolInfo(
                        collection = order.token,
                        curve = data.bondingCurve,
                        spotPrice = data.spotPrice,
                        delta = data.delta,
                        fee = data.fee,
                        protocolFee = sudoSwapProtocolFeeProvider.getProtocolFeeMultiplier(data.factory)
                    )
                }
            }
        } else null
    }

    private suspend fun getInfoFromChain(poolAddress: Address): PoolInfo {
        val contract = LSSVMPairV1(poolAddress, sender)
        return coroutineScope {
            val collection = async { contract.nft().call().awaitSingle() }
            val spotPrice = async { contract.spotPrice().call().awaitSingle() }
            val delta = async { contract.delta().call().awaitSingle() }
            val curve = async { contract.bondingCurve().call().awaitSingle() }
            val fee = async { contract.fee().call().awaitSingle() }
            val protocolFee = async {
                val factory = contract.factory().call().awaitSingle()
                sudoSwapProtocolFeeProvider.getProtocolFeeMultiplier(factory)
            }
            PoolInfo(
                collection = collection.await(),
                curve = curve.await(),
                spotPrice = spotPrice.await(),
                delta = delta.await(),
                fee =fee.await(),
                protocolFee = protocolFee.await()
            )
        }
    }
}